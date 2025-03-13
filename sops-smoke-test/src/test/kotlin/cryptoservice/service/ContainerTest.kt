import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer

class ContainerTest {
    @Test
    fun `call encrypt`() {
        val address = "http://${cryptoServiceContainer.host}:${cryptoServiceContainer.getMappedPort(8080)}"

        val expectedDetailRegex =
            Regex(
                "Exception message: Failed when encrypting RiSc with ID: some-id by running sops command: sops --encrypt" +
                    " --input-type json --output-type yaml --config /tmp/sopsConfig-some-id-\\d+.yaml /dev/stdin with error " +
                    "message: config file not found, or has no creation rules, and no keys provided through command line options\n",
            )
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
                    "config": "some-config: 1",
                    "gcpAccessToken": "fake-token",
                    "riScId": "some-id"
                }
                """.trimIndent(),
            ).exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(SimpleErrorResponse::class.java)
            .value({ errorResponse ->
                assertTrue(errorResponse.body.detail?.matches(expectedDetailRegex) ?: false, {
                    """
                    Unexpected error detail:
                    Expected ${expectedDetailRegex.pattern}
                    actual: ${errorResponse.body.detail}
                    """.trimIndent()
                })
            })
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
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            cryptoServiceContainer.stop()
        }
    }
}
