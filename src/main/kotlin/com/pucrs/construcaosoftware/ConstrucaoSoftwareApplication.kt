package com.pucrs.construcaosoftware

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "API", version = "1.0", description = "vamo ve essa coisa"))
class ConstrucaoSoftwareApplication

fun main(args: Array<String>) {
	runApplication<ConstrucaoSoftwareApplication>(*args)
}
