package eu.auctionplatform.commons.domain

/**
 * Enumeration of supported brands / tenants on the auction platform.
 * Includes both EU and US market brands.
 *
 * Each brand carries a short [code] that is used as a routing key in NATS
 * subjects and as a discriminator column in multi-tenant data stores.
 */
enum class Brand(val code: String) {

    TROOSTWIJK("troostwijk"),
    SURPLEX("surplex"),
    INDUSTRIAL_AUCTIONS("industrial-auctions"),
    SPC("spc"),
    SPIN_LOGISTICS("spin-logistics"),
    CUSTOM("custom"),

    // US market brands
    RITCHIE_BROS("ritchie-bros"),
    PURPLE_WAVE("purple-wave"),
    BIDADOO("bidadoo"),
    GOVPLANET("govplanet"),
    PROXIBID("proxibid"),
    IRON_PLANET("iron-planet");

    companion object {

        private val byCode: Map<String, Brand> = entries.associateBy { it.code.lowercase() }

        /**
         * Resolves a [Brand] from its [code] (case-insensitive).
         *
         * @throws IllegalArgumentException if the code is not recognised.
         */
        fun fromCode(code: String): Brand =
            byCode[code.lowercase()] ?: throw IllegalArgumentException(
                "Unknown brand code '$code'. Known codes: ${byCode.keys}"
            )
    }
}
