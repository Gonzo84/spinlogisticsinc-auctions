dependencies {
    // JPA entities with Panache active-record pattern
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    // REST client for Adyen PSP integration and VIES VAT API
    implementation("io.quarkus:quarkus-rest-client-jackson")
}
