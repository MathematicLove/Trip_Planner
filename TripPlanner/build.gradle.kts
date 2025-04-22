plugins {
    java
    application
    id("io.spring.dependency-management") version "1.1.7"
}

apply(plugin = "io.spring.dependency-management")

group = "org.tripplanner"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    // so you can run with `./gradlew run`
    mainClass.set("org.tripplanner.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    // Core Spring
    implementation("org.springframework:spring-context:6.1.4")
    implementation("org.springframework:spring-webflux:6.1.4")

    // Spring Data JPA & Hibernate
    implementation("org.springframework.data:spring-data-jpa:3.2.4")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework:spring-orm:6.1.4")
    implementation("org.hibernate:hibernate-core:6.2.7.Final")
    implementation("org.postgresql:postgresql:42.6.0")

    // HikariCP for connection pooling
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Reactive MongoDB
    implementation("org.springframework.data:spring-data-mongodb:4.2.4")
    implementation("org.mongodb:mongodb-driver-reactivestreams:5.4.0")
    implementation("org.reactivestreams:reactive-streams:1.0.4")

    // Kafka
    implementation("org.apache.kafka:kafka-streams:3.7.0")
    implementation("org.springframework.kafka:spring-kafka:3.1.2")

    // Spring JMS (if you decide to use it)
    implementation("org.springframework:spring-jms:6.1.4")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-api:1.3.4")

    // Testing
    testImplementation("org.springframework:spring-test:6.1.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

dependencyManagement {
    imports {
        // Align all Spring artifacts to 6.1.4
        mavenBom("org.springframework:spring-framework-bom:6.1.4")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
