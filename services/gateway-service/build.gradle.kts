dependencies {
    // WebSockets for proxying real-time connections
    implementation("io.quarkus:quarkus-websockets-next")
    // Redis for rate limiting and webhook deduplication
    implementation("io.quarkus:quarkus-redis-client")
    // REST client for proxying requests to backend services
    implementation("io.quarkus:quarkus-rest-client-jackson")
}
