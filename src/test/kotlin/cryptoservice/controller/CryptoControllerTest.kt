package cryptoservice.controller

import cryptoservice.model.GCPAccessToken
import cryptoservice.model.RiScWithConfig
import cryptoservice.model.SopsConfig
import cryptoservice.service.DecryptionService
import cryptoservice.service.EncryptionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*


//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //starter spring-applikasjonen
//inkludert controllere. må ha den for å ha et endepunkt å kalle
@WebMvcTest(CryptoController::class)
@AutoConfigureMockMvc //setter opp en mockMvc automatisk
@TestPropertySource(
    properties = [
        "sops.ageKey=test-age-key",
        "sops.decryption.backendPublicKey=dummy-backend",
        "sops.decryption.securityTeamPublicKey=dummy-team",
        "sops.decryption.securityPlatformPublicKey=dummy-platform"
    ]
)

class CryptoControllerTest() {

    @Autowired //initialiserer webtestclient
    lateinit var mockMvc: MockMvc // ønsker en webtestclient koblet til applikasjonen når jeg starter testen

    //TODO: bruke denne eller ReplaceWithMock? hvilken spring boot versjon brukes og skal det oppdateres snart?

    @MockBean
    lateinit var decryptionService: DecryptionService

    @MockBean
    lateinit var encryptionService: EncryptionService

    @Value("\${sops.ageKey}") //dummy variabel definert over
    lateinit var sopsAgePrivateKey: String

    // teste decrypt endepunktet
    // én success
    @Test
    //fun `should return 200 ok and decrypted object`() { // TODO: kanskje endre navn på testen
    fun `should return RiscWithConfig object on successful decrypt`() { //funker dette?
        val cipherText = "ENC[encrypted]"
        val token = "token123"

        val expected = RiScWithConfig(
            riSc = "decrypted-data",
            sopsConfig = SopsConfig(
                shamir_threshold = 1
                //gcp_kms = emptyList(),
                //version = "3.7.3"
            )
        )

        `when`(
            decryptionService.decryptWithSopsConfig(
                ciphertext = cipherText,
                gcpAccessToken = GCPAccessToken(token),
                sopsAgeKey = sopsAgePrivateKey
            )
        ).thenReturn(expected)

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
            .andExpect(jsonPath("$.risc").value("decrypted-data"))
            //.andExpect(jsonPath("$.sopsConfig.version").value("3.7.3"))
    }
}

                // Arrange

                // Act
                // Assert

        // én fail
        //@Test
        //fun `should return 200 ok and decrypted object`() {

       // }

    // teste encrypt endepunktet
        // én success
        // én fail


