plugins {
    kotlin("jvm")
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    // NATS client
    implementation("io.nats:jnats:2.24.1")

    // Casbin for authorization
    implementation("org.casbin:jcasbin:1.93.0")
    implementation("org.casbin:jdbc-adapter:2.13.0")

    // Jakarta EE APIs (for JAX-RS annotations, CDI)
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:4.0.0")
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0")
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("jakarta.validation:jakarta.validation-api:3.1.0")
    compileOnly("org.eclipse.microprofile.config:microprofile-config-api:3.1")

    // Quarkus Jackson (for ObjectMapperCustomizer interface)
    compileOnly("io.quarkus:quarkus-jackson:3.30.6")

    // Agroal DataSource (for OutboxPoller JDBC access)
    compileOnly("io.quarkus:quarkus-agroal:3.30.6")

    // JBoss Logging (standard Quarkus logging)
    compileOnly("org.jboss.logging:jboss-logging:3.6.1.Final")

    // SLF4J for MDC (structured logging context in consumers)
    compileOnly("org.slf4j:slf4j-api:2.0.16")

    // Micrometer for optional metrics (provided by quarkus-micrometer at runtime)
    compileOnly("io.micrometer:micrometer-core:1.14.4")

    // Test
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.16")
}
