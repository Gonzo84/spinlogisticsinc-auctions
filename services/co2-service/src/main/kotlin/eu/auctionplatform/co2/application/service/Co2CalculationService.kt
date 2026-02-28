package eu.auctionplatform.co2.application.service

import eu.auctionplatform.co2.domain.model.Co2Calculation
import eu.auctionplatform.co2.domain.model.EmissionFactor
import eu.auctionplatform.co2.infrastructure.persistence.repository.Co2CalculationRepository
import eu.auctionplatform.co2.infrastructure.persistence.repository.EmissionFactorRepository
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.commons.util.JsonMapper
import io.nats.client.Connection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Application service responsible for CO2 avoidance calculations.
 *
 * Calculates the estimated CO2 avoided by reusing an asset through the
 * auction platform instead of manufacturing a new replacement. The
 * calculation uses emission factors stored per product category.
 */
@ApplicationScoped
class Co2CalculationService {

    @Inject
    lateinit var emissionFactorRepository: EmissionFactorRepository

    @Inject
    lateinit var co2CalculationRepository: Co2CalculationRepository

    @Inject
    lateinit var natsConnection: Connection

    companion object {
        private val LOG: Logger = Logger.getLogger(Co2CalculationService::class.java)
    }

    // -------------------------------------------------------------------------
    // Calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates the CO2 avoided for a specific lot based on its category.
     *
     * The formula is: `co2Avoided = newManufacturingCo2Kg * reuseFactor`
     *
     * After calculation, the result is persisted and a `co2.calculated.{lotId}`
     * event is published to NATS for downstream consumers.
     *
     * @param lotId      The catalog lot identifier.
     * @param categoryId The category identifier for emission factor lookup.
     * @param sellerId   Optional seller identifier for the lot-seller mapping.
     * @return The CO2 calculation result.
     */
    fun calculateForLot(lotId: UUID, categoryId: UUID, sellerId: UUID? = null): Co2Calculation {
        val factor = emissionFactorRepository.findByCategoryId(categoryId)

        if (factor == null) {
            LOG.warnf("No emission factor found for category [%s] -- using zero CO2", categoryId)
            val calculation = Co2Calculation(
                id = IdGenerator.generateUUIDv7(),
                lotId = lotId,
                categoryId = categoryId,
                co2AvoidedKg = BigDecimal.ZERO,
                calculatedAt = Instant.now(),
                version = 1
            )
            co2CalculationRepository.insert(calculation)
            return calculation
        }

        // Determine version (increment if recalculating)
        val existing = co2CalculationRepository.findByLotId(lotId)
        val version = (existing?.version ?: 0) + 1

        val co2Avoided = factor.newManufacturingCo2Kg
            .multiply(factor.reuseFactor)
            .setScale(2, RoundingMode.HALF_UP)

        val calculation = Co2Calculation(
            id = IdGenerator.generateUUIDv7(),
            lotId = lotId,
            categoryId = categoryId,
            co2AvoidedKg = co2Avoided,
            calculatedAt = Instant.now(),
            version = version
        )

        co2CalculationRepository.insert(calculation)

        // Publish event to NATS
        publishCo2CalculatedEvent(calculation, sellerId)

        LOG.infof("CO2 calculated for lot [%s]: %s kg avoided (factor=%s, reuse=%s)",
            lotId, co2Avoided, factor.newManufacturingCo2Kg, factor.reuseFactor)

        return calculation
    }

    // -------------------------------------------------------------------------
    // Summaries
    // -------------------------------------------------------------------------

    /**
     * Returns the platform-wide CO2 avoidance summary.
     *
     * @return A summary containing total CO2 avoided and total lots processed.
     */
    fun getPlatformSummary(): Co2Summary {
        val (totalCo2, totalLots) = co2CalculationRepository.getPlatformTotal()

        return Co2Summary(
            totalCo2AvoidedKg = totalCo2,
            totalLots = totalLots,
            equivalentTreesPlanted = calculateTreeEquivalent(totalCo2),
            equivalentCarKmAvoided = calculateCarKmEquivalent(totalCo2)
        )
    }

    /**
     * Returns a CO2 avoidance summary for a specific seller.
     *
     * @param sellerId The seller's user identifier.
     * @return A summary for the seller.
     */
    fun getSellerSummary(sellerId: UUID): Co2Summary {
        val (totalCo2, totalLots) = co2CalculationRepository.getSellerTotal(sellerId)

        return Co2Summary(
            totalCo2AvoidedKg = totalCo2,
            totalLots = totalLots,
            equivalentTreesPlanted = calculateTreeEquivalent(totalCo2),
            equivalentCarKmAvoided = calculateCarKmEquivalent(totalCo2)
        )
    }

    // -------------------------------------------------------------------------
    // Emission factor management
    // -------------------------------------------------------------------------

    /**
     * Returns the emission factor with the given ID, or null if not found.
     *
     * @param id The emission factor identifier.
     * @return The emission factor, or null.
     */
    fun getEmissionFactor(id: UUID): EmissionFactor? {
        return emissionFactorRepository.findById(id)
    }

    /**
     * Returns all emission factors.
     */
    fun getAllEmissionFactors(): List<EmissionFactor> {
        return emissionFactorRepository.findAll()
    }

    /**
     * Updates an existing emission factor.
     *
     * @param id     The emission factor identifier.
     * @param factor The updated emission factor data.
     * @return The updated emission factor.
     * @throws NotFoundException if the emission factor does not exist.
     */
    fun updateEmissionFactor(id: UUID, factor: EmissionFactor): EmissionFactor {
        val updated = factor.copy(id = id, lastUpdated = Instant.now())
        emissionFactorRepository.update(updated)
        LOG.infof("Updated emission factor: id=%s, type=%s", id, updated.productType)
        return updated
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun publishCo2CalculatedEvent(calculation: Co2Calculation, sellerId: UUID?) {
        try {
            val eventData = mapOf(
                "calculationId" to calculation.id.toString(),
                "lotId" to calculation.lotId.toString(),
                "categoryId" to calculation.categoryId.toString(),
                "co2AvoidedKg" to calculation.co2AvoidedKg.toString(),
                "calculatedAt" to calculation.calculatedAt.toString(),
                "version" to calculation.version,
                "sellerId" to sellerId?.toString()
            )

            val payload = JsonMapper.toJson(eventData).toByteArray(Charsets.UTF_8)
            val subject = "co2.calculated.${calculation.lotId}"

            natsConnection.publish(subject, payload)
            LOG.debugf("Published co2.calculated event for lot [%s]", calculation.lotId)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to publish co2.calculated event for lot [%s]: %s",
                calculation.lotId, ex.message)
            // Do not re-throw: the calculation is already persisted
        }
    }

    /**
     * Estimates the equivalent number of trees planted.
     * Average tree absorbs ~22 kg CO2 per year.
     */
    private fun calculateTreeEquivalent(co2Kg: BigDecimal): BigDecimal {
        val kgPerTree = BigDecimal("22.0")
        return if (co2Kg > BigDecimal.ZERO) {
            co2Kg.divide(kgPerTree, 0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Estimates the equivalent car kilometres avoided.
     * Average car emits ~0.12 kg CO2 per km.
     */
    private fun calculateCarKmEquivalent(co2Kg: BigDecimal): BigDecimal {
        val kgPerKm = BigDecimal("0.12")
        return if (co2Kg > BigDecimal.ZERO) {
            co2Kg.divide(kgPerKm, 0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }
}

/**
 * Summary of CO2 avoidance metrics, including helpful equivalents.
 */
data class Co2Summary(
    val totalCo2AvoidedKg: BigDecimal,
    val totalLots: Long,
    val equivalentTreesPlanted: BigDecimal,
    val equivalentCarKmAvoided: BigDecimal
)
