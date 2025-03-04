package cryptoservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.regex.Pattern

private val logger = LoggerFactory.getLogger(DecryptionService::class.java)

object CryptoValidation {
    fun isValidGCPToken(token: String): Boolean {
        // Regex pattern to match JWT structure: <header>.<payload>.<signature>
        val jwtPattern = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"

        // Check if token matches JWT format
        if (!Pattern.matches(jwtPattern, token)) {
            logger.debug("Invalid GCP Token. Regex fails")
            return false
        }

        // Split the token into parts
        val parts = token.split(".")

        // Ensure there are exactly three parts
        if (parts.size != 3) {
            logger.debug("Invalid GCP Token. Expected 3 parts, found ${parts.size}")
            return false
        }

        // Validate Base64 encoding for each part (allow URL-safe base64 encoding)
        return try {
            // Decode each part and check if the Base64 decoding is valid
            parts.forEach { part ->
                Base64.getUrlDecoder().decode(part)
            }
            true
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid GCP Token. Expected base64-encoded parts")
            false
        }
    }

    fun isValidSopsConfig(config: String): Boolean {
        try {
            // Create ObjectMapper for YAML
            val yamlMapper = ObjectMapper(YAMLFactory())

            // Read YAML and convert to JSON
            yamlMapper.readTree(config)
            return true
        } catch (e: Exception) {
            logger.debug("Expected config to be valid yaml")
            return false
        }
    }

    fun isValidAgePrivateKey(sopsAgeKey: String): Boolean {
        val pattern = "^AGE-SECRET-KEY-1[A-Za-z0-9+/=]+$"

        // Check if the key matches the expected pattern
        if (!Pattern.matches(pattern, sopsAgeKey)) {
            logger.debug("Invalid Age Key. Regex fails")
            return false
        }

        // Extract the base64-encoded part of the key after "age-encryption-key-v1:"
        val base64Key = sopsAgeKey.substring("AGE-SECRET-KEY-1".length)

        // Try decoding the base64-encoded string and check if it's valid base64.
        try {
            Base64.getDecoder().decode(base64Key)
            return true
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid age private key. Expected base64-encoded key")
            return false
        }
    }
}
