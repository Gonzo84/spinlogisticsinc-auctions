package eu.auctionplatform.payment.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.payment.application.service.CheckoutService
import eu.auctionplatform.payment.application.service.LotCheckoutDetail
import eu.auctionplatform.payment.application.service.PaymentWebhookData
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for `auction.lot.awarded` events
 * and initiates the checkout flow for the winning bidder.
 *
 * When a lot is awarded, this consumer:
 * 1. Extracts winner, lot, and price details from the event.
 * 2. Calls [CheckoutService.initiateCheckout] to create payment records.
 * 3. Simulates PSP processing by auto-completing the payment.
 *
 * Uses the durable consumer name `payment-lot-awarded-consumer` to survive
 * restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class LotAwardedConsumer @Inject constructor(
    private val connection: Connection,
    private val checkoutService: CheckoutService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(LotAwardedConsumer::class.java)

        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_NAME = "payment-lot-awarded-consumer"
        private const val FILTER_SUBJECT = "auction.lot.awarded"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "payment-lot-awarded-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting LotAwardedConsumer for payment checkout initiation")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down LotAwardedConsumer")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = FILTER_SUBJECT,
            deadLetterSubject = "dlq.payment.lot-awarded"
        ) {
            override fun handleMessage(message: Message) {
                handleLotAwarded(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun handleLotAwarded(message: Message) {
        val payload = try {
            val json = String(message.data, Charsets.UTF_8)
            JsonMapper.instance.readValue(json, Map::class.java) as Map<String, Any>
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to parse LotAwardedEvent payload: %s", ex.message)
            return
        }

        val winnerId = payload["winnerId"]?.toString()
        val lotId = payload["lotId"]?.toString()
        val auctionId = payload["auctionId"]?.toString()
        val hammerPrice = payload["hammerPrice"]?.toString()?.let { BigDecimal(it) }
        val currency = payload["currency"]?.toString() ?: "EUR"
        val buyerPremiumRate = payload["buyerPremiumRate"]?.toString()?.let { BigDecimal(it) }
            ?: CheckoutService.DEFAULT_BUYER_PREMIUM_RATE

        if (winnerId == null || lotId == null || auctionId == null || hammerPrice == null) {
            LOG.warnf(
                "LotAwardedEvent missing required fields: winnerId=%s, lotId=%s, auctionId=%s, hammerPrice=%s",
                winnerId, lotId, auctionId, hammerPrice
            )
            return
        }

        LOG.infof(
            "Processing LotAwardedEvent: lotId=%s, winnerId=%s, hammerPrice=%s %s",
            lotId, winnerId, hammerPrice, currency
        )

        try {
            val buyerUuid = UUID.fromString(winnerId)
            val lotUuid = UUID.fromString(lotId)
            val auctionUuid = UUID.fromString(auctionId)

            // Build lot checkout detail from event data
            // Seller ID and country info default here; in production they come from lot/catalog service
            val lotDetail = LotCheckoutDetail(
                lotId = lotUuid,
                auctionId = auctionUuid,
                sellerId = auctionUuid, // Placeholder: would be resolved from lot service
                hammerPrice = hammerPrice,
                currency = currency,
                buyerCountry = "NL", // Default: would come from buyer profile
                sellerCountry = "NL", // Default: would come from lot service
                buyerType = "BUSINESS", // Default: would come from buyer profile
                sellerType = "BUSINESS",
                buyerVatId = null
            )

            // 1. Initiate checkout with real event data
            val payments = checkoutService.initiateCheckout(buyerUuid, listOf(lotDetail))

            // 2. Simulate PSP: auto-complete each payment
            for (payment in payments) {
                try {
                    // Process payment (transition to PROCESSING)
                    checkoutService.processPayment(payment.id, "simulated_psp")

                    // Simulate successful webhook from PSP
                    val pspReference = "SIM-${UUID.randomUUID()}"
                    val webhookData = PaymentWebhookData(
                        pspReference = pspReference,
                        merchantReference = payment.id.toString(),
                        eventCode = "AUTHORISATION",
                        success = true,
                        paymentMethod = "simulated_psp",
                        reason = null,
                        amountValue = payment.totalAmount.movePointRight(2).toLong(),
                        amountCurrency = payment.currency
                    )
                    checkoutService.handlePaymentWebhook(webhookData)

                    LOG.infof(
                        "Auto-completed payment %s for lot %s (psp=%s)",
                        payment.id, lotId, pspReference
                    )
                } catch (ex: Exception) {
                    LOG.errorf(
                        ex, "Failed to auto-complete payment %s for lot %s: %s",
                        payment.id, lotId, ex.message
                    )
                }
            }
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to process LotAwardedEvent for lot %s: %s",
                lotId, ex.message
            )
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
