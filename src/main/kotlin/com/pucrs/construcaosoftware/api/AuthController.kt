package com.pucrs.construcaosoftware.api

import com.pucrs.construcaosoftware.dto.LoginDTO
import com.pucrs.construcaosoftware.dto.TokenDTO
import com.pucrs.construcaosoftware.dto.RefreshTokenDTO
import com.pucrs.construcaosoftware.dto.EvaluatePermissionDTO
import com.pucrs.construcaosoftware.keycloak.KeycloakClient
import com.pucrs.construcaosoftware.exceptions.InvalidDataException
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
import kotlin.collections.mapOf

@Configuration
class AuthController {
    @RouterOperations(
        RouterOperation(
            path = "/auth/login",
            method = arrayOf(RequestMethod.POST),
            beanClass = AuthHandler::class,
            beanMethod = "login",
            operation = Operation(
                operationId = "create",
                method = "POST",
                summary = "Login",
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = LoginDTO::class)
                        )
                    )
                )
            ),
        ),
        RouterOperation(
            path = "/auth/refresh_token",
            method = arrayOf(RequestMethod.POST),
            beanClass = AuthHandler::class,
            beanMethod = "refreshToken",
            operation = Operation(
                operationId = "refreshToken",
                method = "POST",
                summary = "Informe o refresh_token obtido no login para obter um novo token",
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = RefreshTokenDTO::class)
                        )
                    )
                )
            ),
        ),
        RouterOperation(
            path = "/auth/certs",
            method = arrayOf(RequestMethod.GET),
            beanClass = AuthHandler::class,
            beanMethod = "certs",
            operation = Operation(
                operationId = "certs",
                method = "GET",
                summary = "Recupera as chaves p??blicas do servidor, no formato JWK, para valida????o dos tokens",
            ),
        ),
        RouterOperation(
            path = "/auth/evaluate_permission",
            method = arrayOf(RequestMethod.POST),
            beanClass = AuthHandler::class,
            beanMethod = "evaluatePermission",
            operation = Operation(
                operationId = "evaluatePermission",
                method = "POST",
                summary = "Verifica se o usu??rio tem permiss??o para acessar determinada rota",
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = EvaluatePermissionDTO::class)
                        )
                    )
                )
            ),
        )
    )
  @Bean
  fun authRoutes(handler: AuthHandler): RouterFunction<ServerResponse> =
    RouterFunctions.route(RequestPredicates.POST("/auth/login")) {
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
        .andRoute(RequestPredicates.POST("/auth/refresh_token")) {
            handler.refreshToken(it).flatMap{ r ->
                ServerResponse.ok().bodyValue(r)
            }
            .onErrorResume(WebClientResponseException::class.java) { e ->
                ServerResponse
                    .status(e.statusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.responseBodyAsString)
            }
        }
        .andRoute(RequestPredicates.GET("/auth/certs")) {
            handler.certs(it).flatMap{ r ->
                ServerResponse.ok().bodyValue(r)
            }
            .onErrorResume(WebClientResponseException::class.java) { e ->
                ServerResponse
                    .status(e.statusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.responseBodyAsString)
            }
        }
        .andRoute(RequestPredicates.POST("/auth/evaluate_permission")) {
            handler.evaluatePermission(it).flatMap{ r ->
                ServerResponse.ok().bodyValue(r)
            }
            .onErrorResume(WebClientResponseException::class.java) { e ->
                ServerResponse
                    .status(e.statusCode)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.responseBodyAsString)
            }.onErrorResume(InvalidDataException::class.java) { e ->
                ServerResponse
                    .status(400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(mapOf("error" to e.message))
            }
        }
}

@Component
class AuthHandler(private val service: AuthService) {
    fun login(request: ServerRequest) = request.bodyToMono(LoginDTO::class.java).flatMap { u -> service.login(u) }
    fun refreshToken(request: ServerRequest) = request.bodyToMono(RefreshTokenDTO::class.java).flatMap { u -> service.refreshToken(u) }
    fun certs(request: ServerRequest) = service.certs()
    fun evaluatePermission(request: ServerRequest) = request.bodyToMono(EvaluatePermissionDTO::class.java).flatMap { u -> service.evaluatePermission(u) }
}

@Service
class AuthService(private val keycloakClient: KeycloakClient) {
    fun login(dto: LoginDTO): Mono<TokenDTO> = keycloakClient.login(dto.username, dto.password)
    fun refreshToken(dto: RefreshTokenDTO): Mono<TokenDTO> = keycloakClient.refreshToken(dto.refresh_token)
    fun certs(): Mono<Any> = keycloakClient.certs()
    fun evaluatePermission(dto: EvaluatePermissionDTO): Mono<Any> = keycloakClient.evaluatePermission(dto.userToken, dto.resource, dto.scope)
}
