dependencies {
    // Real-time bid updates via WebSockets
    implementation("io.quarkus:quarkus-websockets-next")
    // Caching auction state (current bids, timers)
    implementation("io.quarkus:quarkus-redis-client")
}
