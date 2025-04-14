package cryptoservice.controller

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.ninjasquad.springmockk.MockkBean

@WebMvcTest(CryptoController::class)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = ["sops.ageKey=test-age-key"] //ha denne med? eller hente fra application.properties?
)

class CryptoControllerTest() {

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
        //arrange
        val cipherText = "ENC[encrypted]"
        val token = "token123"

        val expected = RiScWithConfig(
            riSc = "decrypted-data",
            sopsConfig = SopsConfig(
                shamir_threshold = 1,
                version = "3.7.3"
            )
        )

        println("sopsAgePrivateKey = '$sopsAgePrivateKey'")
        every {
            decryptionService.decryptWithSopsConfig(
                ciphertext = match {
                    println("ACTUAL ciphertext: '$it'")
                    it.trim('"') == cipherText
                },
                gcpAccessToken = eq(GCPAccessToken(token)),
                sopsAgeKey = eq(sopsAgePrivateKey)
            )
        } returns expected

        mockMvc.perform(
            post("/decrypt")
                .header("gcpAccessToken", token)
                .content("\"$cipherText\"")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo { result ->
                println("Response body: ${result.response.contentAsString}")
            }

            .andExpect(status().isOk)
            .andExpect(jsonPath("$.riSc").value("decrypted-data"))
            .andExpect(jsonPath("$.sopsConfig.version").value("3.7.3"))
    }
}

        // én fail
        //@Test
        //fun `should return 200 ok and decrypted object`() {

       // }

    // teste encrypt endepunktet
        // én success
        // én fail


