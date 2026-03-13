package eu.auctionplatform.commons.messaging

/**
 * Centralised constants for all NATS subject names used across the platform.
 *
 * Convention: `<domain>.<entity>.<verb>` (dot-separated, lowercase).
 *
 * Subjects can be scoped to a brand / tenant via [withBrand], which inserts the
 * brand code as a prefix segment, e.g. `troostwijk.auction.bid.placed`.
 */
object NatsSubjects {

    // ── Auction / Bidding ────────────────────────────────────────────────
    const val AUCTION_BID_PLACED: String        = "auction.bid.placed"
    const val AUCTION_BID_PROXY: String         = "auction.bid.proxy"
    const val AUCTION_LOT_EXTENDED: String      = "auction.lot.extended"
    const val AUCTION_LOT_CLOSED: String        = "auction.lot.closed"
    const val AUCTION_BID_REJECTED: String       = "auction.bid.rejected"
    const val AUCTION_DEPOSIT_REQUIRED: String   = "auction.deposit.required"
    const val AUCTION_RESERVE_MET: String        = "auction.reserve.met"
    const val AUCTION_LOT_AWARDED: String        = "auction.lot.awarded"
    const val AUCTION_LOT_AWARD_REVOKED: String  = "auction.lot.award-revoked"
    const val AUCTION_CANCELLED: String          = "auction.cancelled"

    // ── Catalog ──────────────────────────────────────────────────────────
    const val CATALOG_LOT_CREATED: String       = "catalog.lot.created"
    const val CATALOG_LOT_UPDATED: String        = "catalog.lot.updated"
    const val CATALOG_LOT_STATUS_CHANGED: String = "catalog.lot.status_changed"

    // ── Payment ──────────────────────────────────────────────────────────
    const val PAYMENT_CHECKOUT_COMPLETED: String = "payment.checkout.completed"
    const val PAYMENT_SETTLEMENT_READY: String   = "payment.settlement.ready"
    const val PAYMENT_DEPOSIT_PAID: String       = "payment.deposit.paid"
    const val PAYMENT_SETTLEMENT_SETTLED: String = "payment.settlement.settled"
    const val PAYMENT_DEPOSIT_REFUNDED: String   = "payment.deposit.refunded"

    // ── User / Identity ──────────────────────────────────────────────────
    const val USER_REGISTERED: String           = "user.registered"
    const val USER_KYC_VERIFIED: String         = "user.kyc.verified"
    const val USER_BUYER_BLOCKED: String       = "user.buyer.blocked"

    // ── Media ────────────────────────────────────────────────────────────
    const val MEDIA_IMAGE_PROCESSED: String     = "media.image.processed"
    const val MEDIA_IMAGE_UPLOADED: String      = "media.image.uploaded"

    // ── Notifications ────────────────────────────────────────────────────
    const val NOTIFY_OVERBID: String            = "notify.overbid"
    const val NOTIFY_CLOSING_SOON: String       = "notify.closing.soon"

    // ── Compliance ───────────────────────────────────────────────────────
    const val COMPLIANCE_GDPR_ERASURE: String   = "compliance.gdpr.erasure"

    // ── CO₂ / Sustainability ─────────────────────────────────────────────
    const val CO2_CALCULATED: String            = "co2.calculated"

    // ── Payment Non-Payment ───────────────────────────────────────────────
    const val PAYMENT_NON_PAYMENT_PENALTY: String = "payment.non-payment.penalty"
    const val PAYMENT_LOT_RELIST: String          = "payment.lot.relist-requested"

    /**
     * Prefixes a NATS [subject] with the given [brand] code.
     *
     * @deprecated Brand should be sent as NATS header, not as subject prefix.
     *             The AUCTION stream captures `auction.>` and brand-prefixed subjects
     *             (e.g. `troostwijk.auction.bid.placed`) silently fall outside the stream.
     */
    @Deprecated("Brand should be sent as NATS header, not as subject prefix", level = DeprecationLevel.WARNING)
    fun withBrand(subject: String, brand: String): String = "$brand.$subject"
}
