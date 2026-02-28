package eu.auctionplatform.gateway.infrastructure.webhook

import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// =============================================================================
// Webhook Validator – HMAC signature verification for incoming webhooks
// =============================================================================

/**
 * Validates HMAC signatures for incoming webhook payloads from third-party
 * providers (Adyen, Onfido).
 *
 * Each provider uses a slightly different HMAC scheme:
 * - **Adyen**: HMAC-SHA256 with Base64-encoded key and signature.
 * - **Onfido**: HMAC-SHA256 with hex-encoded signature.
 *
 * All validation methods use constant-time comparison to prevent timing attacks.
 */
@ApplicationScoped
class WebhookValidator {

    companion object {
        private val LOG: Logger = Logger.getLogger(WebhookValidator::class.java)

        private const val HMAC_SHA256 = "HmacSHA256"
    }

    // -------------------------------------------------------------------------
    // Adyen HMAC Validation
    // -------------------------------------------------------------------------

    /**
     * Validates an Adyen webhook HMAC signature.
     *
     * Adyen signs notification payloads using HMAC-SHA256 with a Base64-encoded
     * HMAC key. The computed signature is compared against the provided
     * [signature] (also Base64-encoded).
     *
     * @param payload   The raw webhook payload body (JSON string).
     * @param signature The `HmacSignature` value from the Adyen notification
     *                  `additionalData` map.
     * @param key       The Adyen HMAC key (Base64-encoded, configured per merchant account).
     * @return `true` if the signature is valid.
     */
    fun validateAdyenHmac(payload: String, signature: String, key: String): Boolean {
        return try {
            val keyBytes = Base64.getDecoder().decode(key)
            val computed = computeHmacSha256Base64(payload.toByteArray(Charsets.UTF_8), keyBytes)
            constantTimeEquals(computed, signature)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Adyen HMAC validation error: %s", ex.message)
            false
        }
    }

    // -------------------------------------------------------------------------
    // Onfido HMAC Validation
    // -------------------------------------------------------------------------

    /**
     * Validates an Onfido webhook HMAC signature.
     *
     * Onfido signs webhook payloads using HMAC-SHA256 with a raw string token.
     * The computed signature is hex-encoded and compared against the
     * `X-SHA2-Signature` header value.
     *
     * @param payload   The raw webhook payload body (JSON string).
     * @param signature The value of the `X-SHA2-Signature` HTTP header.
     * @param token     The Onfido webhook token (plain string).
     * @return `true` if the signature is valid.
     */
    fun validateOnfidoHmac(payload: String, signature: String, token: String): Boolean {
        return try {
            val computed = computeHmacSha256Hex(payload.toByteArray(Charsets.UTF_8), token.toByteArray(Charsets.UTF_8))
            constantTimeEquals(computed, signature)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Onfido HMAC validation error: %s", ex.message)
            false
        }
    }

    // -------------------------------------------------------------------------
    // Generic HMAC Validation
    // -------------------------------------------------------------------------

    /**
     * Generic HMAC-SHA256 validation with Base64-encoded output.
     *
     * Useful for any provider that follows the standard HMAC-SHA256 + Base64
     * pattern.
     *
     * @param payload   The raw payload bytes to verify.
     * @param signature The expected Base64-encoded HMAC signature.
     * @param keyBytes  The raw HMAC key bytes.
     * @return `true` if the computed signature matches.
     */
    fun validateHmacBase64(payload: ByteArray, signature: String, keyBytes: ByteArray): Boolean {
        return try {
            val computed = computeHmacSha256Base64(payload, keyBytes)
            constantTimeEquals(computed, signature)
        } catch (ex: Exception) {
            LOG.errorf(ex, "HMAC-SHA256 (Base64) validation error: %s", ex.message)
            false
        }
    }

    // -------------------------------------------------------------------------
    // HMAC computation
    // -------------------------------------------------------------------------

    /**
     * Computes HMAC-SHA256 and returns the result as a Base64-encoded string.
     */
    private fun computeHmacSha256Base64(data: ByteArray, key: ByteArray): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        val hash = mac.doFinal(data)
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Computes HMAC-SHA256 and returns the result as a lowercase hex string.
     */
    private fun computeHmacSha256Hex(data: ByteArray, key: ByteArray): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        val hash = mac.doFinal(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    // -------------------------------------------------------------------------
    // Constant-time comparison
    // -------------------------------------------------------------------------

    /**
     * Compares two strings in constant time to prevent timing attacks.
     *
     * Returns `false` immediately only if lengths differ (which is already
     * leaked by typical HMAC schemes). For equal-length strings, every byte
     * is compared regardless of mismatches.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
