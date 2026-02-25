dependencies {
    // Elasticsearch low-level REST client
    implementation("io.quarkus:quarkus-elasticsearch-rest-client")
    // Elasticsearch Java API client
    implementation("co.elastic.clients:elasticsearch-java:8.12.0")
    // Jackson Kotlin & Java Time modules for ES client deserialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
