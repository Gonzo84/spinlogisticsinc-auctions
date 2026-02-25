package eu.auctionplatform.search.infrastructure.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.apache.http.HttpHost
import org.apache.http.HttpRequestInterceptor
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * CDI producer for the Elasticsearch Java API [ElasticsearchClient].
 *
 * The Elasticsearch Java client (8.x / 9.x) sends vendor-specific
 * content-type headers (`application/vnd.elasticsearch+json; compatible-with=8`)
 * by default. Elasticsearch 8.x can reject these with a
 * `media_type_header_exception` ("Invalid media-type value on headers").
 *
 * To work around this, an [HttpRequestInterceptor] is registered on the
 * low-level Apache [org.elasticsearch.client.RestClient] that replaces any
 * Content-Type and Accept headers with plain `application/json` **after**
 * the transport layer has set its vendor-specific values, ensuring the
 * outgoing request always uses the standard MIME type.
 */
@ApplicationScoped
class ElasticsearchClientProducer {

    @ConfigProperty(name = "quarkus.elasticsearch.hosts", defaultValue = "localhost:9200")
    lateinit var esHosts: String

    @Produces
    @Singleton
    fun produceElasticsearchClient(): ElasticsearchClient {
        val hosts = esHosts.split(",").map { host ->
            val parts = host.trim().split(":")
            HttpHost(parts[0], parts.getOrElse(1) { "9200" }.toInt(), "http")
        }.toTypedArray()

        val restClient = org.elasticsearch.client.RestClient.builder(*hosts)
            .setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.addInterceptorLast(HttpRequestInterceptor { request, _ ->
                    // Remove all vendor-specific Content-Type / Accept headers
                    // set by the ES Java client transport and replace with plain JSON.
                    request.removeHeaders("Content-Type")
                    request.removeHeaders("Accept")
                    request.addHeader("Content-Type", "application/json")
                    request.addHeader("Accept", "application/json")
                })
            }
            .build()

        val objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val transport = RestClientTransport(restClient, JacksonJsonpMapper(objectMapper))
        return ElasticsearchClient(transport)
    }
}
