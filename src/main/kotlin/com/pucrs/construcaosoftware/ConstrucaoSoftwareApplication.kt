package com.pucrs.construcaosoftware

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "Keyseguro", version = "1.0.3", description = "API de autenticação e gerenciamento de usuários"))
class ConstrucaoSoftwareApplication

fun main(args: Array<String>) {
	runApplication<ConstrucaoSoftwareApplication>(*args)
}
