package eu.auctionplatform.commons.domain

/**
 * Enumeration of supported brands / tenants on the auction platform.
 *
 * Each brand carries a short [code] that is used as a routing key in NATS
 * subjects and as a discriminator column in multi-tenant data stores.
 */
enum class Brand(val code: String) {

    TROOSTWIJK("troostwijk"),
    SURPLEX("surplex"),
    INDUSTRIAL_AUCTIONS("industrial-auctions"),
    SPC("spc"),
    CUSTOM("custom");

    companion object {

        private val byCode: Map<String, Brand> = entries.associateBy { it.code }

        /**
         * Resolves a [Brand] from its [code].
         *
         * @throws IllegalArgumentException if the code is not recognised.
         */
        fun fromCode(code: String): Brand =
            byCode[code] ?: throw IllegalArgumentException(
                "Unknown brand code '$code'. Known codes: ${byCode.keys}"
            )
    }
}
