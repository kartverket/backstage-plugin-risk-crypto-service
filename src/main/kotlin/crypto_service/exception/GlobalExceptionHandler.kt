package crypto_service.exception

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
    fun handleUncaughtException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {

        logger.warn("Exception thrown: $1", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }
}