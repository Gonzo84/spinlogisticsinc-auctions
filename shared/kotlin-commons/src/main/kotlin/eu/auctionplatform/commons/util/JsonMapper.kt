package eu.auctionplatform.commons.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Singleton [ObjectMapper] pre-configured for the auction platform.
 *
 * Configuration:
 * - Kotlin module registered (handles data classes, default values, etc.)
 * - JSR-310 (java.time) module registered; dates serialised as ISO-8601 strings
 * - Unknown JSON properties are silently ignored on deserialisation
 * - Null fields are omitted from serialised output
 */
object JsonMapper {

    val instance: ObjectMapper = jacksonObjectMapper().apply {
        // java.time support (Instant, LocalDate, …)
        registerModule(JavaTimeModule())

        // Write dates as ISO-8601 strings, not numeric timestamps
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        // Do not fail when the JSON contains fields the target class does not have
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        // Omit null values from serialised JSON
        setSerializationInclusion(JsonInclude.Include.NON_NULL)

        // Pretty-print is intentionally left disabled for production throughput.
    }

    /**
     * Serialises [value] to a JSON string.
     */
    fun toJson(value: Any): String = instance.writeValueAsString(value)

    /**
     * Deserialises a JSON [json] string into an instance of [T].
     */
    inline fun <reified T> fromJson(json: String): T =
        instance.readValue(json, T::class.java)
}
