import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/* ─── версии в одном месте ─────────────────────────────────────────────── */
val springVersion      = "6.1.5"
val springDataJpaVer   = "3.2.5"
val springDataMongoVer = "4.2.5"
val modulithVer        = "1.1.1"
val hibernateVer       = "6.4.4.Final"

/* ─── плагины ───────────────────────────────────────────────────────────── */
plugins {
    java
    application
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("war")
}

/* ─── общие настройки ──────────────────────────────────────────────────── */
group   = "org.tripplanner"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))   // ← LTS JDK 21
}

application {
    mainClass.set("org.tripplanner.Main")
}

repositories { mavenCentral() }

/* ─── зависимости ──────────────────────────────────────────────────────── */
dependencies {

    // Spring Core / Web MVC (без Boot)
    implementation("org.springframework:spring-context:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("org.springframework:spring-webmvc:$springVersion")
    implementation("org.springframework:spring-jdbc:$springVersion")
    implementation("org.springframework:spring-tx:$springVersion")
    implementation("org.springframework:spring-orm:$springVersion")
    implementation("org.springframework:spring-webflux:$springVersion")
    annotationProcessor("org.springframework:spring-context-indexer:$springVersion")

    //
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.5")
    implementation("org.apache.tomcat.embed:tomcat-embed-jasper:10.1.16")
    implementation("jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0")
    implementation("org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1")

    // Spring Modulith
    implementation("org.springframework.modulith:spring-modulith-starter-core:$modulithVer")

    // JPA / Hibernate
    implementation("org.springframework.data:spring-data-jpa:$springDataJpaVer")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.hibernate.orm:hibernate-core:$hibernateVer")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Reactive MongoDB
    implementation("org.springframework.data:spring-data-mongodb:$springDataMongoVer")
    implementation("org.mongodb:mongodb-driver-reactivestreams:5.1.0")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka:3.1.2")
    implementation("org.apache.kafka:kafka-clients:3.6.1")

    // Jetty (embedded HTTP server)
    implementation("org.eclipse.jetty:jetty-server:11.0.17")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.17")

    // Reactor + JSON
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.17.0")

    // UI
    implementation("jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0")
    runtimeOnly   ("org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1")
    runtimeOnly("org.eclipse.jetty:apache-jsp:11.0.17")
    implementation("jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.1")
    runtimeOnly("org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1")
    // Тесты
    testImplementation("org.springframework:spring-test:$springVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

/* ─── BOM выравнивает версии spring-артефактов ─────────────────────────── */
dependencyManagement {
    imports {
        mavenBom("org.springframework:spring-framework-bom:$springVersion")
    }
}

/* ─── тесты ─────────────────────────────────────────────────────────────── */
tasks.withType<Test> { useJUnitPlatform() }

/* ─── Shadow-fat-jar (artifact) ─────────────────────────────────────────── */
tasks.withType<ShadowJar> {
    archiveClassifier.set("")                         // → tripplanner-1.0.0.jar
    manifest.attributes["Main-Class"] = application.mainClass.get()
}

/* делаем `jar` → `shadowJar`, чтобы CI получал сразу fat-jar */
tasks.named("jar") {
    dependsOn(tasks.named("shadowJar"))
    enabled = false
}

tasks.processResources{
    from("src/main/webapp")
}

sourceSets {
    main {
        // вместе с обычными ресурсами забираем и webapp
        resources.srcDir("src/main/webapp")
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
