package eu.auctionplatform.search.infrastructure.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.elasticsearch.client.RestClient

/**
 * CDI producer for the Elasticsearch Java API [ElasticsearchClient].
 *
 * Wraps the Quarkus-managed low-level [RestClient] (provided by
 * `quarkus-elasticsearch-rest-client`) with the high-level Java API transport.
 */
@ApplicationScoped
class ElasticsearchClientProducer {

    @Produces
    @Singleton
    fun produceElasticsearchClient(restClient: RestClient): ElasticsearchClient {
        val transport = RestClientTransport(restClient, JacksonJsonpMapper())
        return ElasticsearchClient(transport)
    }
}
