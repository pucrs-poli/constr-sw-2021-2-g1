package com.pucrs.construcaosoftware.api

import com.pucrs.construcaosoftware.dto.LoginDTO
import com.pucrs.construcaosoftware.dto.TokenDTO
import com.pucrs.construcaosoftware.keycloak.KeycloakClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.stereotype.Service
import org.springframework.stereotype.Component
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import reactor.core.publisher.Mono
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.parameters.RequestBody
import org.springframework.http.MediaType

@Configuration
class AuthController {
  @RouterOperations(
    RouterOperation(
      path = "/login",
      method = arrayOf(RequestMethod.POST),
      beanClass = AuthHandler::class,
      beanMethod = "login",
      operation = Operation(
          operationId = "create",
          method = "POST",
          requestBody = RequestBody(
              required = true,
              content = arrayOf(
                  Content(
                      schema = Schema(implementation = LoginDTO::class)
                  )
              )
          )
      ),
    )
  )
  @Bean
  fun authRoutes(handler: AuthHandler): RouterFunction<ServerResponse> =
    RouterFunctions.route(RequestPredicates.POST("/login")) {
        handler.login(it).flatMap{ r ->
          ServerResponse.ok().bodyValue(r)
        }
        .onErrorResume(WebClientResponseException::class.java) { e ->
            ServerResponse
                .status(e.statusCode)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(e.responseBodyAsString)
        }
    }
}

@Component
class AuthHandler(private val service: AuthService) {
  fun login(request: ServerRequest) = request.bodyToMono(LoginDTO::class.java).flatMap { u -> service.login(u) }
}

@Service
class AuthService(private val keycloakClient: KeycloakClient) {
  fun login(dto: LoginDTO): Mono<TokenDTO> = keycloakClient.login(dto.username, dto.password)
}
