import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer

class ContainerTest {
    @Test
    fun `call encrypt`() {
        val address = "http://${cryptoServiceContainer.host}:${cryptoServiceContainer.getMappedPort(8080)}"

        val client = WebTestClient.bindToServer().baseUrl(address).build()
        client
            .post()
            .uri("/encrypt")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                    "text": "Hello",
                    "config": {"gcp_kms": null},
                    "gcpAccessToken": "fake-token",
                    "riScId": "some-id"
                }
                """.trimIndent(),
            ).exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(SimpleErrorResponse::class.java)
            .value({ errorResponse ->
                println("ACTUAL RESPONSE: ${errorResponse.body.detail}")
                assertEquals(
                    "Exception message: Failed when encrypting RiSc with ID: some-id ",
                    errorResponse.body.detail,
                )
            })
    }

    @Test
    fun `check that sops exists as a command in container`() {
        val sopsResult: Container.ExecResult = cryptoServiceContainer.execInContainer("sops", "--version")
        assertEquals(0, sopsResult.exitCode)
    }

    class SimpleErrorResponse(
        val body: ProblemDetail,
    )

    companion object {
        val cryptoServiceContainer =
            GenericContainer(System.getenv("CRYPTO_SERVICE_CONTAINER") ?: "crypto-service-test:latest")
                .withExposedPorts(8080)

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            cryptoServiceContainer.start()
            val logger = LoggerFactory.getLogger(ContainerTest::class.java)
            val logConsumer = Slf4jLogConsumer(logger).withSeparateOutputStreams()
            cryptoServiceContainer.followOutput(logConsumer)
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            cryptoServiceContainer.stop()
        }
    }
}
