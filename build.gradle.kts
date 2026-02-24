plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.allopen") version "2.3.0" apply false
    kotlin("plugin.noarg") version "2.3.0" apply false
    id("io.quarkus") apply false
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

allprojects {
    group = "eu.auctionplatform"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

// ── Shared configuration for all Kotlin subprojects ──────────────────────────
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        "implementation"(kotlin("stdlib"))
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
        "implementation"("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

        "testImplementation"(kotlin("test"))
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
        "testImplementation"("io.mockk:mockk:1.13.16")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
            javaParameters = true
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    configure<org.gradle.api.plugins.JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// ── Quarkus service configuration ────────────────────────────────────────────
// Applied to all projects under services/
configure(subprojects.filter { it.path.startsWith(":services:") }) {
    apply(plugin = "io.quarkus")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
    apply(plugin = "org.jetbrains.kotlin.plugin.noarg")

    dependencies {
        // Quarkus BOM
        "implementation"(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

        // Core Quarkus
        "implementation"("io.quarkus:quarkus-kotlin")
        "implementation"("io.quarkus:quarkus-arc")
        "implementation"("io.quarkus:quarkus-config-yaml")
        "implementation"("io.quarkus:quarkus-rest")
        "implementation"("io.quarkus:quarkus-rest-jackson")
        "implementation"("io.quarkus:quarkus-jackson")
        "implementation"("io.quarkus:quarkus-scheduler")
        "implementation"("io.quarkus:quarkus-cache")

        // Auth
        "implementation"("io.quarkus:quarkus-oidc")
        "implementation"("io.quarkus:quarkus-smallrye-jwt")

        // Database
        "implementation"("io.quarkus:quarkus-jdbc-postgresql")
        "implementation"("io.quarkus:quarkus-agroal")
        "implementation"("io.quarkus:quarkus-flyway")
        "implementation"("org.flywaydb:flyway-database-postgresql")
        "runtimeOnly"("org.postgresql:postgresql")

        // NATS
        "implementation"("io.nats:jnats:2.24.1")

        // Casbin RBAC
        "implementation"("org.casbin:jcasbin:1.93.0")
        "implementation"("org.casbin:jdbc-adapter:2.13.0")

        // Observability
        "implementation"("io.quarkus:quarkus-opentelemetry")
        "implementation"("io.quarkus:quarkus-logging-json")
        "implementation"("io.quarkus:quarkus-micrometer-registry-prometheus")
        "implementation"("io.quarkus:quarkus-smallrye-health")
        "runtimeOnly"("io.opentelemetry:opentelemetry-exporter-sender-okhttp")

        // API docs
        "implementation"("io.quarkus:quarkus-smallrye-openapi")

        // Validation
        "implementation"("io.quarkus:quarkus-hibernate-validator")

        // Error handling
        "implementation"("io.quarkiverse.resteasy-problem:quarkus-resteasy-problem:3.21.0")

        // Container image
        "implementation"("io.quarkus:quarkus-container-image-jib")

        // Shared libraries
        "implementation"(project(":shared:kotlin-commons"))
        "implementation"(project(":shared:nats-events"))

        // Test
        "testImplementation"("io.quarkus:quarkus-junit5")
        "testImplementation"("io.rest-assured:rest-assured")
        "testImplementation"("org.testcontainers:testcontainers")
        "testImplementation"("org.testcontainers:junit-jupiter")
        "testImplementation"("org.testcontainers:postgresql")
        "testImplementation"("io.quarkus:quarkus-test-security-oidc")
    }

    the<io.quarkus.gradle.extension.QuarkusPluginExtension>().apply {
        // Quarkus extension configuration if needed
    }

    // allOpen for Quarkus
    configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
        annotation("jakarta.ws.rs.Path")
        annotation("jakarta.enterprise.context.ApplicationScoped")
        annotation("jakarta.enterprise.context.RequestScoped")
        annotation("jakarta.persistence.Entity")
        annotation("io.quarkus.test.junit.QuarkusTest")
    }

    // noArg for JPA entities
    configure<org.jetbrains.kotlin.noarg.gradle.NoArgExtension> {
        annotation("jakarta.persistence.Entity")
    }
}
