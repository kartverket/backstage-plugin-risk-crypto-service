package cryptoservice.exception.exceptions

data class SopsEncryptionException(
    override val message: String,
    val riScId: String,
    val errorCode: String? = null,
) : Exception()

data class SOPSDecryptionException(
    override val message: String,
    val errorCode: String? = null,
) : Exception()
