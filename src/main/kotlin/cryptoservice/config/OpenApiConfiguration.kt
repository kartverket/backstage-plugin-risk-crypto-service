package cryptoservice.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun customOpenAPI(
        @Value("\${spring.application.name}") applicationName: String,
    ): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Crypto Service API")
                    .version("1.0")
                    .description(
                        """
                        API for encrypting and decrypting sensitive data using SOPS (Secrets OPerationS).
                        
                        This service provides endpoints for:
                        - Encrypting plain text with SOPS configuration
                        - Decrypting SOPS-encrypted data
                        
                        The service uses Age encryption and supports GCP KMS integration.
                        """.trimIndent(),
                    ).contact(
                        Contact()
                            .name("Kartverket")
                            .url("https://github.com/kartverket"),
                    ).license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT"),
                    ),
            )
}
