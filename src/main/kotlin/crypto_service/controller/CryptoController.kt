package crypto_service.controller

import crypto_service.model.GCPAccessToken
import crypto_service.service.DecryptionService
import crypto_service.service.EncryptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CryptoController(
    val encryptionService: EncryptionService,
    val decryptionService: DecryptionService
) {

    @GetMapping("/decrypt")
    fun decrypt(
        ciphertext: String,
        gcpAccessToken: String,
        agePrivateKey: String
    ): ResponseEntity<String> {

        val decryptedString = decryptionService.decrypt(ciphertext, GCPAccessToken(gcpAccessToken), agePrivateKey)

        return ResponseEntity.ok().body(decryptedString)
    }

    @GetMapping("/encrypt")
    fun encrypt(
        text: String,
        config: String,
        gcpAccessToken: String,
        riScId: String
    ): ResponseEntity<String> {

        val encryptedString = encryptionService.encrypt(text, config, GCPAccessToken(gcpAccessToken), riScId)

        return ResponseEntity.ok().body(encryptedString)
    }
}
