package cryptoservice.controller

import cryptoservice.controller.models.EncryptionRequest
import cryptoservice.model.GCPAccessToken
import cryptoservice.service.DecryptionService
import cryptoservice.service.EncryptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class CryptoController(
    val encryptionService: EncryptionService,
    val decryptionService: DecryptionService,
) {
    /* Dette er slik vi egentlig burde gjøre dekryptering, for at crypto-service kan være helt uavhengig av
     * tjenesten som kaller den. Ettersom SOPS + age er avhengig av at det enten finnes en keys.txt-fil eller
     * at `SOPS_AGE_KEY` er satt som miljøvariabel, så kan vi ikke gjøre det på denne måten enda. Det er mulig dette
     * kan bli støttet i fremtiden ifølge dokumentasjonen til SOPS.
     */
    @GetMapping("/decrypt-with-age-key")
    fun decryptWithAgeKey(
        @RequestHeader gcpAccessToken: String,
        @RequestHeader agePrivateKey: String,
        @RequestBody cipherText: String,
    ): ResponseEntity<String> {
        // Bruk decryptionService.decrypt() og legg til agePrivateKey som inputparameter.

        return ResponseEntity("Not implemented yet", HttpStatus.NOT_IMPLEMENTED)
    }

    @GetMapping("/decrypt")
    fun decrypt(
        @RequestHeader gcpAccessToken: String,
        @RequestBody cipherText: String,
    ): ResponseEntity<String> {
        val decryptedString = decryptionService.decrypt(cipherText, GCPAccessToken(gcpAccessToken))

        return ResponseEntity.ok().body(decryptedString)
    }

    @PostMapping("/encrypt")
    fun encrypt(
        @RequestBody request: EncryptionRequest,
    ): ResponseEntity<String> {
        val encryptedString =
            encryptionService.encrypt(
                request.text,
                request.config,
                GCPAccessToken(request.gcpAccessToken),
                request.riScId,
            )

        return ResponseEntity.ok().body(encryptedString)
    }
}
