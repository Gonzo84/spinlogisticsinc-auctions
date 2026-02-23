package eu.auctionplatform.auction.application.service

import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.commons.domain.AuctionId
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Scheduled job that polls for auctions whose end time has passed and
 * triggers the closing process.
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
    private val lifecycleService: AuctionLifecycleService
) {

    private val logger = LoggerFactory.getLogger(AuctionClosingScheduler::class.java)

    /**
     * Polls for auctions that should be closed.
     *
     * Executes every 1 second. Each auction is closed independently; failures
     * on one auction do not prevent processing of others in the batch.
     */
    @Scheduled(every = "1s", identity = "auction-closing-scheduler")
    fun checkAndCloseAuctions() {
        val now = Instant.now()

        val auctionsToClose = readModelRepository.findActiveAuctionsClosingBefore(now)
        if (auctionsToClose.isEmpty()) {
            return
        }

        logger.info("Found {} auction(s) eligible for closing", auctionsToClose.size)

        for (auction in auctionsToClose) {
            try {
                val auctionId = AuctionId(auction.auctionId)
                val result = lifecycleService.closeAuction(auctionId)
                logger.info(
                    "Successfully closed auction {} (winner={}, finalBid={}, reserveMet={})",
                    auctionId, result.winnerId, result.finalBid, result.reserveMet
                )
            } catch (ex: Exception) {
                logger.error(
                    "Failed to close auction {}: {}",
                    auction.auctionId, ex.message, ex
                )
                // Continue processing remaining auctions -- this one will be
                // retried on the next scheduler tick if it is still eligible.
            }
        }
    }
}
