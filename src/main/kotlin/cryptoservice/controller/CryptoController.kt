package cryptoservice.controller

import cryptoservice.controller.models.EncryptionRequest
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.RiScWithConfig
import cryptoservice.service.DecryptionService
import cryptoservice.service.EncryptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Crypto", description = "Encryption and decryption operations")
class CryptoController(
    val encryptionService: EncryptionService,
    val decryptionService: DecryptionService,
    @Value("\${sops.ageKey}") val sopsAgePrivateKey: String,
) {
    @PostMapping("/decrypt")
    @Operation(summary = "Decrypt data", description = "Decrypt ciphertext using SOPS")
    fun decryptPost(
        @Parameter(description = "GCP access token") @RequestHeader gcpAccessToken: String,
        @RequestBody cipherText: String,
    ): ResponseEntity<RiScWithConfig> {
        val plainTextAndConfig =
            decryptionService.decryptWithSopsConfig(
                ciphertext = cipherText,
                gcpAccessToken = GCPAccessToken(gcpAccessToken),
                sopsAgeKey = sopsAgePrivateKey,
            )
        return ResponseEntity.ok().body(plainTextAndConfig)
    }

    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt data", description = "Encrypt plaintext using SOPS")
    fun encryptPost(
        @RequestBody request: EncryptionRequest,
    ): ResponseEntity<String> {
        val encryptedString =
            encryptionService.encrypt(
                text = request.text,
                config = request.config,
                gcpAccessToken = GCPAccessToken(request.gcpAccessToken),
                riScId = request.riScId,
            )

        return ResponseEntity.ok().body(encryptedString)
    }
}
