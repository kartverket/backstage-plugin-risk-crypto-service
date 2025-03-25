plugins {
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
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

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:testcontainers:1.20.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    dependsOn("buildDockerIfNeeded")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar").enabled = false
tasks.getByName<Jar>("jar").enabled = true

tasks.register("buildDockerIfNeeded") {

    doLast {

        if (System.getenv("CI") != null) {
            println("Running in CI/CD, skipping Docker build.")
            return@doLast
        }

        val dockerProcess = ProcessBuilder("docker", "info").start()
        if (dockerProcess.waitFor() != 0) {
            throw GradleException("Docker is not running. Please start Docker and try again.")
        }

        val imageExists =
            ProcessBuilder("docker", "images", "-q", "crypto-service-test:latest")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .trim()
                .isNotEmpty()

        if (!imageExists) {
            println("Docker image 'crypto-service-test:latest' not found. Building...")
            val arch =
                if (System.getProperty("os.arch") == "aarch64") {
                    "arm64"
                } else {
                    "amd64"
                }
            ProcessBuilder("docker", "build", "--build-arg", "TARGETARCH=$arch", "-t", "crypto-service-test", ".")
                .start()
                .run {
                    val result = this.errorReader().readText()
                    if (waitFor() == 0) {
                        println("image built successfully")
                    } else {
                        logger.error("Docker build failed with ${exitValue()}: $result")
                        throw IllegalStateException("Docker build failed with ${exitValue()}: $result")
                    }
                }
        } else {
            println("Docker image 'crypto-service-test:latest' already exists. Skipping build.")
        }
    }
}
