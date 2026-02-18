package cryptoservice.exception

import cryptoservice.exception.exceptions.SOPSDecryptionException
import cryptoservice.exception.exceptions.SopsEncryptionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handleUncaughtException(ex: Exception): ResponseEntity<Map<String, Any?>> {
        logger.warn(ex.message, ex)
        val body =
            mapOf(
                "errorMessage" to (ex.message ?: "An unexpected error occurred"),
                "errorCode" to "INTERNAL_SERVER_ERROR",
            )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body)
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SopsEncryptionException::class)
    fun handleSopsEncryptionException(ex: SopsEncryptionException): ResponseEntity<Map<String, Any?>> {
        logger.error(ex.message, ex)
        val body =
            mapOf(
                "errorMessage" to ex.message,
                "errorCode" to ex.errorCode,
            )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body)
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SOPSDecryptionException::class)
    fun handleSopsDecryptionException(ex: SOPSDecryptionException): ResponseEntity<Map<String, Any?>> {
        logger.error(ex.message, ex)
        val body =
            mapOf(
                "errorMessage" to ex.message,
                "errorCode" to ex.errorCode,
            )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body)
    }
}
