package cryptoservice.controller

import cryptoservice.controller.models.EncryptionRequest
import cryptoservice.model.GCPAccessToken
import cryptoservice.service.DecryptionService
import cryptoservice.service.EncryptionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CryptoController(
    val encryptionService: EncryptionService,
    val decryptionService: DecryptionService,
    @Value("\${sops.ageKey}") val sopsAgePrivateKey: String,
) {
    @PostMapping("/decrypt")
    fun decryptPost(
        @RequestHeader gcpAccessToken: String,
        @RequestBody cipherText: String,
    ): ResponseEntity<String> {
        val decryptedString =
            decryptionService.decrypt(
                ciphertext = cipherText,
                gcpAccessToken = GCPAccessToken(gcpAccessToken),
                sopsAgeKey = sopsAgePrivateKey,
            )

        return ResponseEntity.ok().body(decryptedString)
    }

    @PostMapping("/encrypt")
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
