package eu.auctionplatform.auction.application.service

import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.commons.domain.AuctionId
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Scheduled job that polls for auctions whose end time has passed and
 * triggers the closing process, followed by auto-award when conditions are met.
 *
 * Runs every second to ensure timely auction closure. Each invocation
 * queries the read model for active auctions with an end time at or
 * before the current instant, then delegates to [AuctionLifecycleService]
 * to close each one.
 *
 * Anti-sniping extensions may push the end time forward, so a previously
 * identified auction may no longer be eligible for closing on the next poll.
 * The closing command is idempotent at the aggregate level -- invoking close
 * on an already-closed auction throws an exception that is caught and logged.
 *
 * ## Error handling
 *
 * Each auction is closed independently in its own try/catch block. Failures
 * on one auction (e.g. concurrency conflict during anti-sniping, aggregate
 * already closed) do not prevent processing of the remaining auctions in the
 * same batch. Failed auctions will be retried on the next scheduler tick.
 */
@ApplicationScoped
class AuctionClosingScheduler @Inject constructor(
    private val readModelRepository: AuctionReadModelRepository,
    private val lifecycleService: AuctionLifecycleService,
    @ConfigProperty(name = "auction.auto-award.enabled", defaultValue = "true")
    private val autoAwardEnabled: String,
    @ConfigProperty(name = "auction.auto-award.delay-seconds", defaultValue = "0")
    private val autoAwardDelaySeconds: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionClosingScheduler::class.java)
    }

    /**
     * Polls for auctions that should be closed.
     *
     * Executes every 1 second. Each auction is closed independently; failures
     * on one auction do not prevent processing of others in the batch.
     *
     * After closing, if auto-award is enabled and the auction is eligible
     * (has winner + reserve met), the lot is automatically awarded.
     */
    @Scheduled(every = "1s", identity = "auction-closing-scheduler")
    fun checkAndCloseAuctions() {
        val now = Instant.now()

        val auctionsToClose = readModelRepository.findActiveAuctionsClosingBefore(now)
        if (auctionsToClose.isEmpty()) {
            return
        }

        LOG.infof("Found %d auction(s) eligible for closing", auctionsToClose.size)

        for (auction in auctionsToClose) {
            try {
                val auctionId = AuctionId(auction.auctionId)
                val result = lifecycleService.closeAuction(auctionId)
                LOG.infof(
                    "Successfully closed auction %s (winner=%s, finalBid=%s, reserveMet=%s)",
                    auctionId, result.winnerId, result.finalBid, result.reserveMet
                )

                // Auto-award if conditions are met
                if (autoAwardEnabled.toBoolean() && result.canAutoAward) {
                    val delaySeconds = autoAwardDelaySeconds.toLongOrNull() ?: 0L
                    if (delaySeconds <= 0) {
                        try {
                            lifecycleService.autoAwardLot(auctionId)
                            LOG.infof("Auto-awarded auction %s", auctionId)
                        } catch (e: Exception) {
                            LOG.errorf(e, "Failed to auto-award auction %s: %s", auctionId, e.message)
                        }
                    } else {
                        LOG.infof("Auction %s eligible for auto-award after %ds delay", auctionId, delaySeconds)
                    }
                }
            } catch (ex: Exception) {
                LOG.errorf(
                    ex,
                    "Failed to close auction %s: %s",
                    auction.auctionId, ex.message
                )
                // Continue processing remaining auctions -- this one will be
                // retried on the next scheduler tick if it is still eligible.
            }
        }
    }

    /**
     * Polls for closed auctions eligible for delayed auto-award.
     *
     * Only active when auto-award delay is configured > 0. Checks every 5 seconds
     * for auctions that closed long enough ago to proceed with auto-award.
     */
    @Scheduled(every = "5s", identity = "delayed-auto-award-scheduler")
    fun checkPendingAutoAwards() {
        if (!autoAwardEnabled.toBoolean()) return
        val delaySeconds = autoAwardDelaySeconds.toLongOrNull() ?: 0L
        if (delaySeconds <= 0) return

        val cutoff = Instant.now().minusSeconds(delaySeconds)
        val closedAuctions = readModelRepository.findClosedAuctionsBeforeWithWinner(cutoff)

        for (auction in closedAuctions) {
            try {
                val auctionId = AuctionId(auction.auctionId)
                lifecycleService.autoAwardLot(auctionId)
                LOG.infof("Delayed auto-award completed for auction %s", auctionId)
            } catch (e: Exception) {
                LOG.errorf(e, "Failed delayed auto-award for %s: %s", auction.auctionId, e.message)
            }
        }
    }
}
