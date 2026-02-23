dependencies {
    // JPA entities with Panache active-record pattern
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    // ORM core (PostGIS support may be added later)
    implementation("io.quarkus:quarkus-hibernate-orm")
}
