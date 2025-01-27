package cryptoservice.service
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val sopsCmd: List<String> = listOf("sops")
val encrypt: List<String> = listOf("encrypt")
val decrypt: List<String> = listOf("decrypt")
val inputTypeYaml: List<String> = listOf("--input-type", "yaml")
val inputTypeJson: List<String> = listOf("--input-type", "json")
val outputTypeYaml: List<String> = listOf("--output-type", "yaml")
val outputTypeJson: List<String> = listOf("--output-type", "json")
val encryptConfig: List<String> = listOf("--encrypt-config")
val inputFile: List<String> = listOf("/dev/stdin")

const val EXECUTION_STATUS_OK = 0

fun gcpAccessToken(accessToken: String): List<String> = listOf("--gcp-access-token", accessToken)

object YamlUtils {
    val yamlFactory = YAMLFactory()
    val objectMapper =
        ObjectMapper(yamlFactory)
            .registerKotlinModule()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    inline fun <reified T> deSerialize(yamlString: String) = objectMapper.readValue(yamlString, T::class.java)

    fun <T> serialize(t: T) =
        ObjectMapper(yamlFactory.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerKotlinModule()
            .writeValueAsString(t)
}
