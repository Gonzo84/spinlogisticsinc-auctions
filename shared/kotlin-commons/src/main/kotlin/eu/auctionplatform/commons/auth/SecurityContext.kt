package eu.auctionplatform.commons.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.Base64

/**
 * Lightweight JWT claim extraction utilities.
 *
 * These extension functions operate on a raw JWT token string (the compact
 * `header.payload.signature` form). They decode the payload segment and
 * extract well-known claims without pulling in a full JWT library – the
 * actual **verification** of signature, expiry, issuer, etc. is expected to
 * happen at the gateway / framework level (e.g. Quarkus OIDC, Spring Security).
 *
 * If a claim is absent the functions return sensible defaults (empty string,
 * empty list, etc.) rather than throwing.
 */

private val objectMapper = jacksonObjectMapper()

/**
 * Decodes the JWT payload (second segment) into a [Map].
 * Returns an empty map if the token is malformed.
 */
@Suppress("UNCHECKED_CAST")
private fun String.decodeClaims(): Map<String, Any> {
    return try {
        val parts = this.split(".")
        if (parts.size < 2) return emptyMap()
        val payloadJson = Base64.getUrlDecoder().decode(parts[1])
        objectMapper.readValue(payloadJson, Map::class.java) as Map<String, Any>
    } catch (_: Exception) {
        emptyMap()
    }
}

/** Extracts the `sub` (subject / user ID) claim. */
fun String.userId(): String =
    decodeClaims()["sub"]?.toString().orEmpty()

/** Extracts the `email` claim from the JWT. */
fun String.email(): String =
    decodeClaims()["email"]?.toString().orEmpty()

/** Extracts the `given_name` (first name) claim from the JWT. */
fun String.givenName(): String =
    decodeClaims()["given_name"]?.toString().orEmpty()

/** Extracts the `family_name` (last name) claim from the JWT. */
fun String.familyName(): String =
    decodeClaims()["family_name"]?.toString().orEmpty()

/** Extracts the `preferred_username` claim from the JWT. */
fun String.preferredUsername(): String =
    decodeClaims()["preferred_username"]?.toString().orEmpty()

/** Extracts the `account_type` custom claim (e.g. "buyer", "seller", "admin"). */
fun String.accountType(): String =
    decodeClaims()["account_type"]?.toString().orEmpty()

/** Extracts the `deposit_status` custom claim (e.g. "paid", "pending", "none"). */
fun String.depositStatus(): String =
    decodeClaims()["deposit_status"]?.toString().orEmpty()

/** Extracts the `kyc_status` custom claim (e.g. "verified", "pending", "rejected"). */
fun String.kycStatus(): String =
    decodeClaims()["kyc_status"]?.toString().orEmpty()

/** Extracts realm or resource `roles` from the token. */
@Suppress("UNCHECKED_CAST")
fun String.roles(): List<String> {
    val claims = decodeClaims()

    // Keycloak-style: realm_access.roles
    val realmAccess = claims["realm_access"] as? Map<String, Any>
    val realmRoles = realmAccess?.get("roles") as? List<String>
    if (!realmRoles.isNullOrEmpty()) return realmRoles

    // Fallback: top-level "roles" claim
    val topLevel = claims["roles"] as? List<String>
    if (!topLevel.isNullOrEmpty()) return topLevel

    return emptyList()
}

/** Extracts `groups` claim (list of group paths, e.g. "/buyers", "/admins"). */
@Suppress("UNCHECKED_CAST")
fun String.groups(): List<String> =
    (decodeClaims()["groups"] as? List<String>).orEmpty()

/**
 * Extracts the `brand_access` custom claim – a list of brand codes the user
 * is authorised to interact with.
 */
@Suppress("UNCHECKED_CAST")
fun String.brandAccess(): List<String> =
    (decodeClaims()["brand_access"] as? List<String>).orEmpty()

/** Extracts the `preferred_language` claim (BCP-47 tag, e.g. "en", "de", "nl"). */
fun String.preferredLanguage(): String =
    decodeClaims()["preferred_language"]?.toString() ?: "en"
