package crypto_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CryptoServiceApplication

fun main(args: Array<String>) {
	runApplication<CryptoServiceApplication>(*args)
}
