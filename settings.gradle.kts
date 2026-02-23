pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
        kotlin("jvm") version "2.3.0"
        kotlin("plugin.allopen") version "2.3.0"
        kotlin("plugin.noarg") version "2.3.0"
    }
}

rootProject.name = "eu-auction-platform"

// ── Shared libraries ─────────────────────────────────────────────────────────
include(":shared:kotlin-commons")
include(":shared:nats-events")

// ── Backend microservices ────────────────────────────────────────────────────
include(":services:auction-engine")
include(":services:catalog-service")
include(":services:user-service")
include(":services:payment-service")
include(":services:notification-service")
include(":services:media-service")
include(":services:search-service")
include(":services:seller-service")
include(":services:broker-service")
include(":services:analytics-service")
include(":services:compliance-service")
include(":services:co2-service")
include(":services:gateway-service")
