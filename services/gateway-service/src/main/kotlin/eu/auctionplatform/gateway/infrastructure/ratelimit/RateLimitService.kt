package eu.auctionplatform.gateway.infrastructure.ratelimit

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.sortedset.ScoreRange
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

// =============================================================================
// Rate Limit Service – Redis-backed sliding-window rate limiter
// =============================================================================

/**
 * Provides per-user rate limiting using Redis sorted sets with a sliding
 * window algorithm.
 *
 * Rate limit tiers:
 * - **General API**: 100 requests/minute per user
 * - **Bid placement**: 10 bids/minute per user per auction
 * - **Search**: 30 requests/minute per user
 *
 * Each request is recorded as a member in a Redis sorted set keyed by
 * `{prefix}:{userId}:{endpoint}`. The score is the request timestamp in
 * milliseconds. Expired entries (outside the window) are pruned on every
 * check to keep memory bounded.
 */
@ApplicationScoped
class RateLimitService @Inject constructor(
    private val redisDataSource: RedisDataSource
) {

    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)

    @ConfigProperty(name = "rate-limit.redis-key-prefix", defaultValue = "rl:")
    lateinit var keyPrefix: String

    @ConfigProperty(name = "rate-limit.enabled", defaultValue = "true")
    var enabled: Boolean = true

    companion object {
        /** General API rate limit: 100 requests per 60 seconds. */
        const val GENERAL_LIMIT: Int = 100
        const val GENERAL_WINDOW_SECONDS: Int = 60

        /** Bid placement rate limit: 10 bids per 60 seconds per auction. */
        const val BID_LIMIT: Int = 10
        const val BID_WINDOW_SECONDS: Int = 60

        /** Search rate limit: 30 requests per 60 seconds. */
        const val SEARCH_LIMIT: Int = 30
        const val SEARCH_WINDOW_SECONDS: Int = 60
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Checks whether the given [userId] has exceeded the rate limit for the
     * specified [endpoint].
     *
     * @param userId        The authenticated user's identifier.
     * @param endpoint      A logical endpoint key (e.g. "api", "bid:{auctionId}", "search").
     * @param limit         Maximum number of requests allowed within the window.
     * @param windowSeconds The sliding window duration in seconds.
     * @return `true` if the request is **allowed** (under the limit), `false` if rate-limited.
     */
    fun checkRateLimit(
        userId: String,
        endpoint: String,
        limit: Int,
        windowSeconds: Int
    ): Boolean {
        if (!enabled) {
            return true
        }

        val key = buildKey(userId, endpoint)
        val now = System.currentTimeMillis()
        val windowStart = now - (windowSeconds * 1000L)

        try {
            val commands = redisDataSource.sortedSet(String::class.java)

            // Remove entries outside the sliding window
            commands.zremrangebyscore(key, ScoreRange.from(Double.NEGATIVE_INFINITY, windowStart.toDouble()))

            // Count current entries within the window
            val currentCount = commands.zcard(key)

            if (currentCount >= limit.toLong()) {
                logger.debug(
                    "Rate limit exceeded for user={}, endpoint={}, count={}/{}",
                    userId, endpoint, currentCount, limit
                )
                return false
            }

            // Add the current request with the timestamp as score
            // Use timestamp + random suffix to avoid duplicate scores
            val member = "$now:${System.nanoTime()}"
            commands.zadd(key, now.toDouble(), member)

            // Set TTL on the key to auto-expire after the window passes
            // This prevents orphaned keys from accumulating
            redisDataSource.key(String::class.java).expire(key, windowSeconds.toLong() + 10)

            logger.debug(
                "Rate limit check passed for user={}, endpoint={}, count={}/{}",
                userId, endpoint, currentCount + 1, limit
            )
            return true
        } catch (ex: Exception) {
            // On Redis failure, allow the request through (fail-open) to avoid
            // blocking all traffic when Redis is temporarily unavailable
            logger.error(
                "Redis error during rate limit check for user={}, endpoint={}: {}",
                userId, endpoint, ex.message, ex
            )
            return true
        }
    }

    /**
     * Checks the general API rate limit for a user.
     */
    fun checkGeneralLimit(userId: String): Boolean =
        checkRateLimit(userId, "api", GENERAL_LIMIT, GENERAL_WINDOW_SECONDS)

    /**
     * Checks the bid placement rate limit for a user on a specific auction.
     */
    fun checkBidLimit(userId: String, auctionId: String): Boolean =
        checkRateLimit(userId, "bid:$auctionId", BID_LIMIT, BID_WINDOW_SECONDS)

    /**
     * Checks the search rate limit for a user.
     */
    fun checkSearchLimit(userId: String): Boolean =
        checkRateLimit(userId, "search", SEARCH_LIMIT, SEARCH_WINDOW_SECONDS)

    /**
     * Returns the number of remaining requests for the given user and endpoint
     * within the current window. Useful for populating rate-limit response headers.
     *
     * @return Number of remaining requests, or -1 if rate limiting is disabled.
     */
    fun remainingRequests(userId: String, endpoint: String, limit: Int, windowSeconds: Int): Long {
        if (!enabled) {
            return -1
        }

        val key = buildKey(userId, endpoint)
        val now = System.currentTimeMillis()
        val windowStart = now - (windowSeconds * 1000L)

        return try {
            val commands = redisDataSource.sortedSet(String::class.java)
            commands.zremrangebyscore(key, ScoreRange.from(Double.NEGATIVE_INFINITY, windowStart.toDouble()))
            val currentCount = commands.zcard(key)
            (limit.toLong() - currentCount).coerceAtLeast(0L)
        } catch (ex: Exception) {
            logger.error("Redis error getting remaining requests: {}", ex.message, ex)
            -1
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private fun buildKey(userId: String, endpoint: String): String =
        "${keyPrefix}${userId}:${endpoint}"
}
