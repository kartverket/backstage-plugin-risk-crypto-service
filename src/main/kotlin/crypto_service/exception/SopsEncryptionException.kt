package crypto_service.exception

data class SopsEncryptionException(
    override val message: String,
    val riScId: String,
) : Exception()
