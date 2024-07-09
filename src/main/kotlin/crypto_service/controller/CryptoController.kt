package crypto_service.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CryptoController {

    @GetMapping("/decrypt")
    fun decrypt(): ResponseEntity<String> {
        //TODO: write decryption functionality

        return ResponseEntity.ok()
            .contentType(MediaType(MediaType.TEXT_HTML, Charsets.UTF_8))
            .body("Decrypted!")
    }

    @GetMapping("/encrypt")
    fun encrypt(): ResponseEntity<String> {
        //TODO: write encryption functionality

        return ResponseEntity.ok()
            .contentType(MediaType(MediaType.TEXT_HTML, Charsets.UTF_8))
            .body("Encrypted!")
    }
}
