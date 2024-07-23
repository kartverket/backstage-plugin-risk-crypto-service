package crypto_service.controller

import crypto_service.model.GCPAccessToken
import crypto_service.service.DecryptionService
import crypto_service.service.EncryptionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


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

        // Hva skjer om decodingen feiler?
        val urlDecodedText = URLDecoder.decode(text, StandardCharsets.UTF_8.toString())

        val encryptedString = encryptionService.encrypt(urlDecodedText, config, GCPAccessToken(gcpAccessToken), riScId)

        return ResponseEntity.ok().body(encryptedString)
    }

//    data class EncryptionRequest(
//        val text: String,
//        val config: String,
//        val gcpAccessToken: String,
//        val riScId: String
//    )

    @PostMapping("/encryptPost")
    fun encryptPost(
        @RequestBody text: String,
        config: String,
        gcpAccessToken: String,
        riScId: String
    ): ResponseEntity<String> {
        val urlDecodedText = URLDecoder.decode(text, StandardCharsets.UTF_8.toString())
        val encryptedString = encryptionService.encrypt(urlDecodedText, config, GCPAccessToken(gcpAccessToken), riScId)
        return ResponseEntity.ok().body(encryptedString)
    }
}
