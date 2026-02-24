dependencies {
    // JPA / Hibernate ORM (required for event-store and snapshot entities)
    implementation("io.quarkus:quarkus-hibernate-orm")
    // Real-time bid updates via WebSockets
    implementation("io.quarkus:quarkus-websockets-next")
    // Caching auction state (current bids, timers)
    implementation("io.quarkus:quarkus-redis-client")
}
