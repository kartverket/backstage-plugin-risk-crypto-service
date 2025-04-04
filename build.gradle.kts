plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.spring") version "2.1.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

// Oppgradeer til siste versjon av ktlint fordi org.jlleitschuh.gradle.ktlint version 12.1.2 bruker for gammel versjon
// Slett når ktlint-plugin'en oppdateres til nyere versjon
ktlint {
    version.set("1.5.0")
}

group = "no"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

sourceSets {
    create("smokeTest") {
        kotlin.srcDirs("src/smokeTest/kotlin")
    }
}

// Shared dependency declaration for all test code
val sharedTestImplementation: Configuration by configurations.register("sharedTestImplementation")
val sharedTestRuntimeOnly: Configuration by configurations.register("sharedTestRuntimeOnly")

// Make the standard tests use the shared test dependencies
val testImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.getByName("sharedTestImplementation"))
}
val testRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.getByName("sharedTestRuntimeOnly"))
}

// Make the smoke tests use the shared test dependencies
val smokeTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.getByName("sharedTestImplementation"))
}
val smokeTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.getByName("sharedTestRuntimeOnly"))
}

val fasterXmlJacksonVersion = "2.18.3"
val kotlinxSerializationVersion = "1.7.3"
val testcontainersVersion = "1.20.6"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$fasterXmlJacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$fasterXmlJacksonVersion")

    sharedTestImplementation("org.springframework.boot:spring-boot-starter-test")
    sharedTestImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    sharedTestRuntimeOnly("org.junit.platform:junit-platform-launcher")

    smokeTestImplementation("org.springframework.boot:spring-boot-starter-webflux")
    smokeTestImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
}

tasks.check {
    dependsOn("smokeTest")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("smokeTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["smokeTest"].output.classesDirs
    classpath = sourceSets["smokeTest"].runtimeClasspath

    dependsOn("buildDockerIfNeeded")

    // The smoke test depends on the container building properly. Should be run after the unit tests if all tests are ran.
    shouldRunAfter("test")
}

tasks.register("buildDockerIfNeeded") {

    doLast {

        if (System.getenv("CI") != null) {
            logger.info("Running in CI/CD, skipping Docker build.")
            return@doLast
        }

        val dockerProcess = ProcessBuilder("docker", "info").start()
        if (dockerProcess.waitFor() != 0) {
            throw IllegalStateException("Docker is not running. Please start Docker and try again.")
        }

        val imageExists =
            ProcessBuilder("docker", "images", "-q", "crypto-service-test:latest")
                .start()
                .inputReader()
                .readText()
                .isNotBlank()

        if (imageExists) {
            logger.info("Docker image 'crypto-service-test:latest' already exists. Skipping build.")
            return@doLast
        }

        logger.info("Docker image 'crypto-service-test:latest' not found. Building...")

        val arch = if (System.getProperty("os.arch") == "aarch64") "arm64" else "amd64"

        ProcessBuilder("docker", "build", "--build-arg", "TARGETARCH=$arch", "-t", "crypto-service-test", ".")
            .start()
            .run {
                val exitCode = waitFor()

                if (exitCode != 0) {
                    val result = errorReader().readText()
                    logger.error("Docker build failed with exit code $exitCode: $result")
                    throw IllegalStateException("Docker build failed with exit code $exitCode: $result")
                }

                logger.info("Docker image built successfully.")
            }
    }
}
