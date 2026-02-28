package eu.auctionplatform.search.infrastructure.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping
import co.elastic.clients.elasticsearch._types.mapping.GeoPointProperty
import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty
import co.elastic.clients.elasticsearch._types.mapping.ObjectProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.ScaledFloatNumberProperty
import co.elastic.clients.elasticsearch._types.mapping.FloatNumberProperty
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.IndexSettings
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis
import co.elastic.clients.elasticsearch._types.analysis.Analyzer
import co.elastic.clients.elasticsearch._types.analysis.CustomAnalyzer
import co.elastic.clients.elasticsearch._types.analysis.TokenFilter
import co.elastic.clients.elasticsearch._types.analysis.SnowballLanguage
import co.elastic.clients.elasticsearch._types.analysis.SnowballTokenFilter
import co.elastic.clients.json.JsonData
import eu.auctionplatform.commons.util.JsonMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.io.StringReader
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Lot Index Service – Elasticsearch index lifecycle management
// =============================================================================

/**
 * Manages the Elasticsearch index for active auction lots.
 *
 * Responsibilities:
 * - Creates the "lots_active" index with multi-language analysers and proper
 *   field mappings on application startup.
 * - Provides CRUD operations for indexing, updating, and deleting lot documents.
 * - Handles index existence checks and re-creation when the mapping changes.
 *
 * The index name is prefixed with the configured [indexPrefix] to support
 * environment isolation (e.g. `auction_lots_active` in production vs
 * `test_lots_active` in tests).
 */
@ApplicationScoped
class LotIndexService @Inject constructor(
    private val esClient: ElasticsearchClient,
    @ConfigProperty(name = "elasticsearch.index.prefix", defaultValue = "auction_")
    private val indexPrefix: String,
    @ConfigProperty(name = "elasticsearch.index.number-of-shards", defaultValue = "1")
    private val numberOfShards: String,
    @ConfigProperty(name = "elasticsearch.index.number-of-replicas", defaultValue = "0")
    private val numberOfReplicas: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(LotIndexService::class.java)

        /** Base index name (combined with prefix at runtime). */
        const val INDEX_BASE_NAME: String = "lots_active"

        /** Archive index for closed / completed lots. */
        const val ARCHIVE_INDEX_BASE_NAME: String = "lots_archive"
    }

    /** Fully qualified active index name including the environment prefix. */
    val activeIndexName: String
        get() = "${indexPrefix}${INDEX_BASE_NAME}"

    /** Fully qualified archive index name including the environment prefix. */
    val archiveIndexName: String
        get() = "${indexPrefix}${ARCHIVE_INDEX_BASE_NAME}"

    // -------------------------------------------------------------------------
    // Index lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates the active lots index if it does not already exist.
     *
     * The index is configured with:
     * - Multi-language text analysers (English, German, Dutch, French) for the
     *   `title` field using sub-fields.
     * - Keyword fields for faceted filtering (category, country, brand, etc.).
     * - Geo-point for proximity/radius searches.
     * - Scaled floats for monetary values (2 decimal precision).
     * - Nested mapping for images to support independent querying.
     */
    fun createIndexIfNotExists() {
        createSingleIndex(activeIndexName)
        createSingleIndex(archiveIndexName)
    }

    private fun createSingleIndex(indexName: String) {
        try {
            val exists = esClient.indices().exists { e -> e.index(indexName) }.value()
            if (exists) {
                LOG.infof("Elasticsearch index [%s] already exists -- skipping creation", indexName)
                return
            }

            LOG.infof("Creating Elasticsearch index [%s] with shards=%s, replicas=%s",
                indexName, numberOfShards, numberOfReplicas)

            esClient.indices().create { c ->
                c.index(indexName)
                    .settings(buildIndexSettings())
                    .mappings(buildMappings())
            }

            LOG.infof("Elasticsearch index [%s] created successfully", indexName)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to create Elasticsearch index [%s]: %s", indexName, ex.message)
            throw ex
        }
    }

    // -------------------------------------------------------------------------
    // Document operations
    // -------------------------------------------------------------------------

    /**
     * Indexes a new lot document or replaces an existing one.
     *
     * @param document The lot document to index.
     */
    fun indexDocument(document: LotDocument) {
        val json = JsonMapper.toJson(document)
        esClient.index<Any> { i ->
            i.index(activeIndexName)
                .id(document.id)
                .withJson(StringReader(json))
        }
        LOG.debugf("Indexed lot document [id=%s] in [%s]", document.id, activeIndexName)
    }

    /**
     * Partially updates an existing lot document using the Elasticsearch update API.
     *
     * Only the fields present in [partialDoc] are merged into the existing document;
     * fields not included remain unchanged.
     *
     * @param documentId The ES document _id (lot ID).
     * @param partialDoc A map of field names to their new values.
     */
    fun updateDocument(documentId: String, partialDoc: Map<String, Any?>) {
        val json = JsonMapper.toJson(partialDoc)
        esClient.update<LotDocument, Any>({ u ->
            u.index(activeIndexName)
                .id(documentId)
                .doc(JsonData.fromJson(json))
                .docAsUpsert(false)
        }, LotDocument::class.java)
        LOG.debugf("Updated lot document [id=%s] in [%s]", documentId, activeIndexName)
    }

    /**
     * Deletes a lot document from the active index.
     *
     * @param documentId The ES document _id to delete.
     */
    fun deleteDocument(documentId: String) {
        esClient.delete { d ->
            d.index(activeIndexName)
                .id(documentId)
        }
        LOG.debugf("Deleted lot document [id=%s] from [%s]", documentId, activeIndexName)
    }

    /**
     * Moves a lot document from the active index to the archive index.
     *
     * This is a two-step operation: first the document is read from the active
     * index, then it is indexed in the archive and removed from active.
     *
     * @param documentId The lot document _id to archive.
     */
    fun archiveDocument(documentId: String) {
        try {
            // Read from active index
            val getResponse = esClient.get({ g ->
                g.index(activeIndexName).id(documentId)
            }, LotDocument::class.java)

            if (!getResponse.found() || getResponse.source() == null) {
                LOG.warnf("Lot document [id=%s] not found in active index for archiving", documentId)
                return
            }

            val document = getResponse.source()!!
            val json = JsonMapper.toJson(document)

            // Index in archive
            esClient.index<Any> { i ->
                i.index(archiveIndexName)
                    .id(documentId)
                    .withJson(StringReader(json))
            }

            // Delete from active
            esClient.delete { d ->
                d.index(activeIndexName).id(documentId)
            }

            LOG.infof("Archived lot document [id=%s] from [%s] to [%s]",
                documentId, activeIndexName, archiveIndexName)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to archive lot document [id=%s]: %s", documentId, ex.message)
            throw ex
        }
    }

    /**
     * Retrieves a lot document by its ID from the active index.
     *
     * @param documentId The ES document _id.
     * @return The [LotDocument] if found, null otherwise.
     */
    fun getDocument(documentId: String): LotDocument? {
        val response = esClient.get({ g ->
            g.index(activeIndexName).id(documentId)
        }, LotDocument::class.java)

        return if (response.found()) response.source() else null
    }

    // -------------------------------------------------------------------------
    // Index settings and mappings
    // -------------------------------------------------------------------------

    /**
     * Builds the index settings including custom multi-language analysers.
     *
     * Four language-specific analysers are configured for the `title` field:
     * - `english_analyzer`: standard tokenizer + English stemmer + lowercase
     * - `german_analyzer`: standard tokenizer + German stemmer + lowercase
     * - `dutch_analyzer`: standard tokenizer + Dutch stemmer + lowercase
     * - `french_analyzer`: standard tokenizer + French stemmer + lowercase
     */
    private fun buildIndexSettings(): IndexSettings {
        return IndexSettings.Builder()
            .numberOfShards(numberOfShards)
            .numberOfReplicas(numberOfReplicas)
            .analysis(buildAnalysis())
            .build()
    }

    /**
     * Builds the analysis configuration with language-specific snowball filters
     * and custom analysers that combine standard tokenization with language stemming.
     */
    private fun buildAnalysis(): IndexSettingsAnalysis {
        return IndexSettingsAnalysis.Builder()
            // Token filters for each language
            .filter("english_stemmer", TokenFilter.Builder()
                .definition(co.elastic.clients.elasticsearch._types.analysis.TokenFilterDefinition.Builder()
                    .snowball(SnowballTokenFilter.Builder()
                        .language(SnowballLanguage.English)
                        .build())
                    .build())
                .build())
            .filter("german_stemmer", TokenFilter.Builder()
                .definition(co.elastic.clients.elasticsearch._types.analysis.TokenFilterDefinition.Builder()
                    .snowball(SnowballTokenFilter.Builder()
                        .language(SnowballLanguage.German)
                        .build())
                    .build())
                .build())
            .filter("dutch_stemmer", TokenFilter.Builder()
                .definition(co.elastic.clients.elasticsearch._types.analysis.TokenFilterDefinition.Builder()
                    .snowball(SnowballTokenFilter.Builder()
                        .language(SnowballLanguage.Dutch)
                        .build())
                    .build())
                .build())
            .filter("french_stemmer", TokenFilter.Builder()
                .definition(co.elastic.clients.elasticsearch._types.analysis.TokenFilterDefinition.Builder()
                    .snowball(SnowballTokenFilter.Builder()
                        .language(SnowballLanguage.French)
                        .build())
                    .build())
                .build())
            // Custom analysers
            .analyzer("english_analyzer", Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("standard")
                    .filter("lowercase", "english_stemmer")
                    .build())
                .build())
            .analyzer("german_analyzer", Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("standard")
                    .filter("lowercase", "german_stemmer")
                    .build())
                .build())
            .analyzer("dutch_analyzer", Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("standard")
                    .filter("lowercase", "dutch_stemmer")
                    .build())
                .build())
            .analyzer("french_analyzer", Analyzer.Builder()
                .custom(CustomAnalyzer.Builder()
                    .tokenizer("standard")
                    .filter("lowercase", "french_stemmer")
                    .build())
                .build())
            .build()
    }

    /**
     * Builds the complete type mapping for the lots index.
     *
     * Field types:
     * - `title`: text with multi-field sub-fields per language analyser
     * - `description`: text (standard analyser)
     * - `categoryId`, `brand`, `country`, `city`, `reserveStatus`, `status`,
     *   `sellerId`, `lotNumber`, `currency`: keyword
     * - `categoryPath`: keyword (array)
     * - `location`: geo_point
     * - `currentBid`, `startingBid`: scaled_float (scaling_factor=100)
     * - `bidCount`: integer
     * - `auctionEndTime`, `createdAt`: date
     * - `co2AvoidedKg`: float
     * - `specifications`: object with dynamic mapping enabled
     * - `images`: nested (url, thumbnailUrl, isPrimary)
     */
    private fun buildMappings(): TypeMapping {
        return TypeMapping.Builder()
            // ----- title: multi-language text field -----
            .properties("title", Property.Builder()
                .text(TextProperty.Builder()
                    .analyzer("standard")
                    .fields(mapOf(
                        "en" to Property.Builder()
                            .text(TextProperty.Builder().analyzer("english_analyzer").build())
                            .build(),
                        "de" to Property.Builder()
                            .text(TextProperty.Builder().analyzer("german_analyzer").build())
                            .build(),
                        "nl" to Property.Builder()
                            .text(TextProperty.Builder().analyzer("dutch_analyzer").build())
                            .build(),
                        "fr" to Property.Builder()
                            .text(TextProperty.Builder().analyzer("french_analyzer").build())
                            .build(),
                        "keyword" to Property.Builder()
                            .keyword(KeywordProperty.Builder().ignoreAbove(512).build())
                            .build()
                    ))
                    .build())
                .build())
            // ----- description: analysed text -----
            .properties("description", Property.Builder()
                .text(TextProperty.Builder()
                    .analyzer("standard")
                    .build())
                .build())
            // ----- categoryId: keyword -----
            .properties("categoryId", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- categoryPath: keyword array -----
            .properties("categoryPath", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- brand: keyword -----
            .properties("brand", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- country: keyword -----
            .properties("country", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- city: keyword -----
            .properties("city", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- location: geo_point -----
            .properties("location", Property.Builder()
                .geoPoint(GeoPointProperty.Builder().build())
                .build())
            // ----- currentBid: scaled_float (scaling_factor=100) -----
            .properties("currentBid", Property.Builder()
                .scaledFloat(ScaledFloatNumberProperty.Builder()
                    .scalingFactor(100.0)
                    .build())
                .build())
            // ----- startingBid: scaled_float (scaling_factor=100) -----
            .properties("startingBid", Property.Builder()
                .scaledFloat(ScaledFloatNumberProperty.Builder()
                    .scalingFactor(100.0)
                    .build())
                .build())
            // ----- bidCount: integer -----
            .properties("bidCount", Property.Builder()
                .integer(IntegerNumberProperty.Builder().build())
                .build())
            // ----- reserveStatus: keyword -----
            .properties("reserveStatus", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- auctionEndTime: date -----
            .properties("auctionEndTime", Property.Builder()
                .date(DateProperty.Builder().build())
                .build())
            // ----- status: keyword -----
            .properties("status", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- co2AvoidedKg: float -----
            .properties("co2AvoidedKg", Property.Builder()
                .float_(FloatNumberProperty.Builder().build())
                .build())
            // ----- specifications: object with dynamic mapping -----
            .properties("specifications", Property.Builder()
                .`object`(ObjectProperty.Builder()
                    .dynamic(DynamicMapping.True)
                    .build())
                .build())
            // ----- images: nested -----
            .properties("images", Property.Builder()
                .nested(NestedProperty.Builder()
                    .properties("url", Property.Builder()
                        .keyword(KeywordProperty.Builder().build())
                        .build())
                    .properties("thumbnailUrl", Property.Builder()
                        .keyword(KeywordProperty.Builder().build())
                        .build())
                    .properties("isPrimary", Property.Builder()
                        .boolean_(BooleanProperty.Builder().build())
                        .build())
                    .build())
                .build())
            // ----- createdAt: date -----
            .properties("createdAt", Property.Builder()
                .date(DateProperty.Builder().build())
                .build())
            // ----- sellerId: keyword -----
            .properties("sellerId", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- lotNumber: keyword -----
            .properties("lotNumber", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            // ----- currency: keyword -----
            .properties("currency", Property.Builder()
                .keyword(KeywordProperty.Builder().build())
                .build())
            .build()
    }
}
