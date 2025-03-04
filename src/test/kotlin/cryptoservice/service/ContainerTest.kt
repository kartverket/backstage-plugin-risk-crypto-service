package cryptoservice.service

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.ComposeContainer
import java.io.File

val compose = ComposeContainer(
    File("docker-compose.yaml")
).withExposedService("crypto-service", 8083)

class ContainerTest {

    @Test
    fun `call encrypt`() {
        val address = "http://" + compose.getServiceHost(
            "crypto-service",
            8083
        ) + ":" + compose.getServicePort("crypto-service", 8083)

        simpleGetRequest(address)
    }

    private fun simpleGetRequest(url: String) {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:8083").build()
        client.get().uri("/")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll(): Unit {
            compose.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll(): Unit {
            compose.stop()
        }
    }
}