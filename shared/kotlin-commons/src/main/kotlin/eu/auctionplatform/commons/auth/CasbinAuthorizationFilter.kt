package eu.auctionplatform.commons.auth

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.casbin.jcasbin.main.Enforcer
import org.jboss.logging.Logger

/**
 * JAX-RS [ContainerRequestFilter] that enforces Casbin RBAC policies.
 *
 * This filter runs **after** Quarkus OIDC authentication and the standard
 * `@RolesAllowed` checks (which run at [Priorities.AUTHORIZATION]). It
 * provides fine-grained path/method-based authorization using the Casbin
 * model and policy files shipped with each service.
 *
 * ## How it works
 *
 * 1. The filter loads `casbin_model.conf` and `casbin_policy.csv` from the
 *    classpath on first use (lazy initialization).
 * 2. For each incoming request it extracts the user's roles from the
 *    [jakarta.ws.rs.core.SecurityContext] (populated by Quarkus OIDC).
 * 3. It checks whether **any** of the user's roles is allowed to perform the
 *    HTTP method on the request path according to the Casbin policy.
 * 4. If none of the roles match, the filter aborts the request with 403.
 *
 * ## Skipped paths
 *
 * Health/readiness probes (`/q/`), webhook endpoints, and `OPTIONS` preflight
 * requests bypass the filter entirely.
 */
@Provider
@Priority(Priorities.AUTHORIZATION + 100) // Run after @RolesAllowed
class CasbinAuthorizationFilter : ContainerRequestFilter {

    companion object {
        private val LOG: Logger = Logger.getLogger(CasbinAuthorizationFilter::class.java)
    }

    /**
     * Lazily initialised Casbin enforcer. Uses the model and policy files
     * from the classpath. If the files are missing or malformed, the enforcer
     * is set to `null` and the filter becomes a no-op (fail-open with a
     * warning log).
     */
    private val enforcer: Enforcer? by lazy {
        try {
            val modelStream = Thread.currentThread().contextClassLoader
                .getResourceAsStream("casbin_model.conf")
            val policyStream = Thread.currentThread().contextClassLoader
                .getResourceAsStream("casbin_policy.csv")

            if (modelStream == null || policyStream == null) {
                LOG.warn(
                    "Casbin model or policy file not found on classpath; " +
                    "authorization filter will be disabled"
                )
                return@lazy null
            }

            // Write streams to temporary files because jCasbin's Enforcer
            // constructor requires file paths, not streams.
            val modelFile = java.io.File.createTempFile("casbin_model", ".conf").apply {
                deleteOnExit()
                writeBytes(modelStream.readBytes())
            }
            val policyFile = java.io.File.createTempFile("casbin_policy", ".csv").apply {
                deleteOnExit()
                writeBytes(policyStream.readBytes())
            }

            Enforcer(modelFile.absolutePath, policyFile.absolutePath).also {
                LOG.info("Casbin enforcer initialised successfully")
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to initialise Casbin enforcer: %s", ex.message)
            null
        }
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.requestUri.rawPath
        val method = requestContext.method

        // Skip non-API paths (health/readiness probes), webhook endpoints, and CORS preflight
        if (path.startsWith("/q/") || path.startsWith("/api/v1/webhooks") || method == "OPTIONS") return

        val e = enforcer ?: return  // fail-open if enforcer not initialized

        val roles = extractRoles(requestContext)
        if (roles.isEmpty()) return  // anonymous or no roles — let @PermitAll/@RolesAllowed handle

        val allowed = roles.any { role -> e.enforce(role, path, method) }
        if (!allowed) {
            LOG.debugf("Casbin denied: roles=%s path=%s method=%s", roles, path, method)
            abortForbidden(requestContext, path, method)
        }
    }

    /**
     * Extracts the roles from the request's security context.
     *
     * Tries multiple strategies:
     * 1. If the SecurityContext is backed by a Quarkus SecurityIdentity, get
     *    roles directly from it.
     * 2. Fall back to checking well-known roles from the platform's role set
     *    via [SecurityContext.isUserInRole].
     */
    private fun extractRoles(requestContext: ContainerRequestContext): Set<String> {
        val sc = requestContext.securityContext ?: return emptySet()

        // Try to get roles via reflection from Quarkus's SecurityIdentity
        try {
            val identityField = sc.javaClass.declaredFields.find { field ->
                field.type.name.contains("SecurityIdentity")
            }
            if (identityField != null) {
                identityField.isAccessible = true
                val identity = identityField.get(sc)
                if (identity != null) {
                    val getRoles = identity.javaClass.getMethod("getRoles")
                    @Suppress("UNCHECKED_CAST")
                    val roles = getRoles.invoke(identity) as? Set<String>
                    if (!roles.isNullOrEmpty()) {
                        return roles
                    }
                }
            }
        } catch (_: Exception) {
            // Reflection failed; fall through to isUserInRole checks
        }

        // Fallback: probe known platform roles
        val knownRoles = listOf(
            "buyer_active", "buyer_pending_kyc", "buyer_blocked",
            "seller_verified", "seller_pending",
            "broker", "broker_active",
            "admin_ops", "admin_super",
            "service_account"
        )

        return knownRoles.filter { sc.isUserInRole(it) }.toSet()
    }

    private fun abortForbidden(
        requestContext: ContainerRequestContext,
        path: String,
        method: String
    ) {
        requestContext.abortWith(
            Response.status(Response.Status.FORBIDDEN)
                .entity(mapOf(
                    "status" to 403,
                    "title" to "Forbidden",
                    "detail" to "Access denied by RBAC policy",
                    "instance" to "$method $path"
                ))
                .type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE)
                .build()
        )
    }
}
