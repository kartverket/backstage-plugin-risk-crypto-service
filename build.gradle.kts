plugins {
    id("org.springframework.boot") version "3.4.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

// Specifies the usage of the currently newest version of Ktlint. `org.jlleitschuh.gradle.ktlint` version 12.2.0 is
// not guaranteed to use the newest version available.
ktlint {
    version.set("1.6.0")
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

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.springframework" && requested.name == "spring-web" && requested.version == "6.2.7") {
            useVersion("6.2.8") // Prøver å fikse sårbarheter i spring-boot-starter-web:3.5.0
        }
        if (requested.group == "org.apache.tomcat.embed" && requested.version == "10.1.41") {
            useVersion("10.1.42")
        }
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

val springBootVersion = "3.4.6"
val fasterXmlJacksonVersion = "2.19.0"
val testcontainersVersion = "1.21.1"
val micrometerVersion = "1.15.1"
val mockkVersion = "1.14.2"
val springMockkVersion = "4.0.2"
val junitVersion = "5.13.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion") {
        because("Provides endpoints for health and event monitoring that are used in SKIP and Docker.")
    }

    implementation("com.fasterxml.jackson:jackson-bom:$fasterXmlJacksonVersion") {
        because("The BOM provides correct versions for all FasterXML Jackson dependencies.")
    }
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")

    sharedTestImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

    platform("org.junit:junit-bom:$junitVersion") {
        because("The BOM (bill of materials) provides correct versions for all JUnit libraries used.")
    }
    sharedTestImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    sharedTestRuntimeOnly("org.junit.platform:junit-platform-launcher")

    smokeTestImplementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
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
