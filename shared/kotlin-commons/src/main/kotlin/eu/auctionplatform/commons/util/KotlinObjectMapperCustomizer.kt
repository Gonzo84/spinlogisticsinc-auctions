package eu.auctionplatform.commons.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.inject.Singleton

/**
 * Quarkus [ObjectMapperCustomizer] that ensures the Jackson Kotlin module
 * is always registered with the platform-managed [ObjectMapper].
 *
 * While `quarkus-kotlin` should auto-register the Kotlin module, this
 * customizer acts as a safety net to guarantee Kotlin data class
 * deserialization works reliably in all environments (dev, test, prod).
 *
 * Without the Kotlin module, RESTEasy Reactive cannot deserialize request
 * bodies into Kotlin data classes that lack a no-arg constructor, resulting
 * in HTTP 400 responses with empty bodies.
 */
@Singleton
class KotlinObjectMapperCustomizer : ObjectMapperCustomizer {

  override fun customize(objectMapper: ObjectMapper) {
    objectMapper.registerModule(KotlinModule.Builder().build())
    objectMapper.registerModule(JavaTimeModule())
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  }
}
