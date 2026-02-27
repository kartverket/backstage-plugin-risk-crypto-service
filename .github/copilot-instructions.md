# Copilot Instructions

## Build, Test, and Lint

```shell
# Build (skip tests)
./gradlew build -x test -x smokeTest

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "cryptoservice.service.EncryptionServiceTest"

# Run a single test method
./gradlew test --tests "cryptoservice.service.EncryptionServiceTest.someMethodName"

# Run smoke tests (requires Docker with image crypto-service-test:latest built)
./gradlew smokeTest

# Run linter
./gradlew ktlintCheck

# Auto-fix lint issues
./gradlew ktlintFormat

# Run the app locally
export SOPS_AGE_KEY=<sops Age private key>
export SOPS_VERSION=<sops version>
./gradlew bootRun --args='--spring.profiles.active=local'
```

The smoke tests spin up the Docker container (`crypto-service-test:latest`) via Testcontainers. Run `docker-compose up` to run the service locally on port 8084 (management on 8083).

## Architecture

This is a Spring Boot 3 Kotlin service that acts as a thin wrapper around the [SOPS](https://github.com/getsops/sops) CLI binary. Its sole purpose is to encrypt and decrypt YAML files (called "RiSc" files) on behalf of the [Backstage risk plugin](https://github.com/kartverket/backstage-plugin-risk-scorecard-backend).

**Encryption/Decryption flow:**
- `CryptoController` exposes two POST endpoints: `/encrypt` and `/decrypt`
- Both endpoints receive a GCP OAuth access token in the request (header for decrypt, body field for encrypt) — this token is passed directly to SOPS as `GOOGLE_OAUTH_ACCESS_TOKEN`
- `EncryptionService` and `DecryptionService` call the `sops` binary via `ProcessBuilder`, piping the payload through `/dev/stdin`
- The SOPS binary must be present on `$PATH` — in production it is compiled from source in the Dockerfile; locally it must be installed manually

**Key configuration (Shamir secret sharing):**
- Encryption always produces three key groups:
  1. Security team Age key + GCP KMS key (from the request's `SopsConfig`)
  2. Backend + security platform Age keys
  3. Developer Age keys from the incoming config (filtered to exclude the service-internal keys)
- The `shamir_threshold` from the incoming `SopsConfig` determines how many key groups are needed for decryption
- The three internal public keys (`backendPublicKey`, `securityTeamPublicKey`, `securityPlatformPublicKey`) are injected via environment variables and bound to `EncryptionService` / `DecryptionService` via `@ConfigurationProperties`

**Ports:**
- `8080` (or `8084` locally) — application endpoints
- `8081` (or `8083` locally) — Spring Actuator (health, prometheus)

**Health check:**
`SopsHealthIndicator` verifies that the `sops` binary exists and its version matches `SOPS_VERSION` env var. The app is DOWN if SOPS is missing or version-mismatched.

**Temp files:**
The SOPS config is written to a temp file with a random Bech32-prefixed name before encryption. It is deleted on success; on failure it may linger in the OS temp directory.

## Git Workflow

- Never commit directly to `main` — always branch off from it
- Branch names use hyphen-separated lowercase words and should be descriptive (e.g., `fix-sops-health-check`, `add-age-key-validation`)
- Commit messages should be descriptive
- Before pushing, auto-fix lint issues: `./gradlew ktlintFormat`

## Key Conventions

- **MockK, not Mockito** — tests use `io.mockk:mockk` and `com.ninja-squad:springmockk`. Don't introduce Mockito.
- **`@ConfigurationProperties` on services** — `EncryptionService` and `DecryptionService` are annotated with both `@Service` and `@ConfigurationProperties`. Public key fields are `lateinit var` properties bound by prefix (`sops.encryption.*` / `sops.decryption.*`).
- **Shared test dependencies** — `sharedTestImplementation` / `sharedTestRuntimeOnly` configurations in `build.gradle.kts` are extended by both `testImplementation` and `smokeTestImplementation`. Add test dependencies there unless they're specific to one suite.
- **Smoke tests are a separate source set** — `src/smokeTest/kotlin` uses Testcontainers to spin up the real Docker image. They run as part of `./gradlew check` but after unit tests.
- **`SopsConfig` uses snake_case field names** with explicit `@JsonProperty` annotations to match the SOPS YAML format exactly. Keep this pattern when modifying the model.
- **Logback is explicitly versioned** — logback-core and logback-classic are excluded from Spring Boot starters and re-added with explicit versions to avoid CVEs. Don't remove these exclusions.
- **To update SOPS version:** change `SOPS_VERSION_ARG` in `Dockerfile` only. The version is baked into the image and validated at runtime by `SopsHealthIndicator`.
- **OpenAPI/Swagger UI** is disabled by default; enable via `ENABLE_API_DOCS=true` env var.
