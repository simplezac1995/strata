plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.strata.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

dependencies {
    // The framework is published to Maven Central; import the BOM to drop explicit versions.
    implementation(platform("io.github.simplezac1995:strata-bom:0.11.4"))
    implementation("io.github.simplezac1995:strata-spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> { useJUnitPlatform() }
