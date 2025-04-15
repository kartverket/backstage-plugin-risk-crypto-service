package cryptoservice.controller

import com.ninjasquad.springmockk.MockkBean
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.RiScWithConfig
import cryptoservice.model.SopsConfig
import cryptoservice.service.DecryptionService
import cryptoservice.service.EncryptionService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CryptoController::class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = ["sops.ageKey=test-age-key"], // ha denne med? eller hente fra application.properties?
)
class CryptoControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var decryptionService: DecryptionService

    @MockkBean
    lateinit var encryptionService: EncryptionService

    @Value("\${sops.ageKey}")
    lateinit var sopsAgePrivateKey: String

    @Test
    fun `should return RiscWithConfig object on successful decrypt`() {
        val cipherText = "ENC[encrypted]"
        val token = "token123"

        val expected =
            RiScWithConfig(
                riSc = "decrypted-data",
                sopsConfig =
                    SopsConfig(
                        shamir_threshold = 1,
                        version = "3.7.3", // trenger strengt tatt ikke ha med denne
                    ),
            )

        every {
            decryptionService.decryptWithSopsConfig(
                ciphertext =
                    match {
                        it.trim('"') == cipherText
                    },
                gcpAccessToken = eq(GCPAccessToken(token)),
                sopsAgeKey = eq(sopsAgePrivateKey),
            )
        } returns expected

        mockMvc
            .perform(
                post("/decrypt")
                    .header("gcpAccessToken", token)
                    .content("\"$cipherText\"")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andDo { result ->
                println("Response body: ${result.response.contentAsString}")
            }.andExpect(status().isOk)
            .andExpect(jsonPath("$.riSc").value("decrypted-data"))
            .andExpect(jsonPath("$.sopsConfig.version").value("3.7.3"))
    }

    @Test
    fun `should return encrypted string on successful encrypt`() {
        val plaintext = "some-secret"
        val encrypted = "ENC[encrypted]"
        val gcpToken = "token123"
        val riScId = "risc-id-123"

        val config =
            SopsConfig(
                shamir_threshold = 1,
            )

        val requestJson =
            """
            {
                "text": "$plaintext",
                "config": {
                    "shamir_threshold": 1
                },
                "gcpAccessToken": "$gcpToken",
                "riScId": "$riScId"
            }
            """.trimIndent()

        every {
            encryptionService.encrypt(
                eq(plaintext),
                eq(config),
                eq(GCPAccessToken(gcpToken)),
                eq(riScId),
            )
        } returns encrypted

        mockMvc
            .perform(
                post("/encrypt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(content().string(encrypted))
    }

    @Test
    fun `should return 400 when decrypt request body is missing`() {
        mockMvc
            .perform(
                post("/decrypt")
                    .header("gcpAccessToken", "token123")
                    .content("")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when encryption request is invalid`() {
        val invalidJson = "{}"

        mockMvc
            .perform(
                post("/encrypt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 500 when decryption fails`() {
        val cipherText = "ENC[invalid]"
        val token = "token123"

        every {
            decryptionService.decryptWithSopsConfig(any(), any(), any())
        } throws RuntimeException("mocked failure")

        mockMvc
            .perform(
                post("/decrypt")
                    .header("gcpAccessToken", token)
                    .content("\"$cipherText\"")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isInternalServerError)
    }

    @Test
    fun `should return 500 when encryption fails`() {
        val plaintext = "some-secret"
        val gcpToken = "token123"
        val riScId = "risc-id-123"

        val requestJson =
            """
            {
                "text": "$plaintext",
                "config": {
                    "shamir_threshold": 1
                },
                "gcpAccessToken": "$gcpToken",
                "riScId": "$riScId"
            }
            """.trimIndent()

        every {
            encryptionService.encrypt(
                any(),
                any(),
                any(),
                any(),
            )
        } throws RuntimeException("mocked encryption failure")

        mockMvc
            .perform(
                post("/encrypt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isInternalServerError)
    }
}
