dependencies {
    // JPA entities with Panache active-record pattern
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    // REST client for Adyen PSP integration and VIES VAT API
    implementation("io.quarkus:quarkus-rest-client-jackson")
    // OIDC client for service-to-service authentication (client credentials flow)
    implementation("io.quarkus:quarkus-oidc-client")
    // Fault tolerance: circuit breaker, retry, timeout for inter-service calls
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
}
