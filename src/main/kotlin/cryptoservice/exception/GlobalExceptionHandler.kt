package cryptoservice.exception

import cryptoservice.exception.exceptions.SOPSDecryptionException
import cryptoservice.exception.exceptions.SopsEncryptionException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handleUncaughtException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn(ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Exception message: ${ex.message}"))
    }

    @ExceptionHandler(SopsEncryptionException::class)
    fun handleSopsEncryptionException(ex: SopsEncryptionException): ResponseEntity<ErrorResponse> {
        logger.error(ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Exception message: ${ex.message}"))
    }

    @ExceptionHandler(SOPSDecryptionException::class)
    fun handleSopsDecryptionException(ex: SOPSDecryptionException): ResponseEntity<ErrorResponse> {
        logger.error(ex.message, ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Exception message: ${ex.message}"))
    }
}
