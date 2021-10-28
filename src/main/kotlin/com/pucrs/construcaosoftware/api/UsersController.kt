package com.pucrs.construcaosoftware.api

import com.pucrs.construcaosoftware.dto.UserCreateDTO
import com.pucrs.construcaosoftware.dto.UserDTO
import com.pucrs.construcaosoftware.dto.UserPartialUpdateDTO
import com.pucrs.construcaosoftware.dto.UserUpdateDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.*
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import com.pucrs.construcaosoftware.keycloak.KeycloakClient

@Configuration
class UsersController {
    @RouterOperations(
        RouterOperation(
            path = "/users",
            method = arrayOf(RequestMethod.POST),
            beanClass = UsersHandler::class,
            beanMethod = "create",
            operation = Operation(
                operationId = "create",
                method = "POST",
                security = [SecurityRequirement(name = "Keycloak access-token")],
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = UserCreateDTO::class)
                        )
                    )
                ),
                responses = [ApiResponse(responseCode = "201", description = "Created")],
            ),
            consumes = arrayOf("application/json"),
            produces = arrayOf("application/json"),
        ),
        RouterOperation(
            path = "/users",
            method = arrayOf(RequestMethod.GET),
            beanClass = UsersHandler::class,
            beanMethod = "list",
            operation = Operation(
                operationId = "list",
                method = "GET",
                security = [SecurityRequirement(name = "Keycloak access-token")],
            ),
            produces = arrayOf("application/json")
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.GET),
            beanClass = UsersHandler::class,
            beanMethod = "get",
            operation = Operation(
                operationId = "get",
                method = "GET",
                security = [SecurityRequirement(name = "Keycloak access-token")],
                parameters = [Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            ),
            produces = arrayOf("application/json")
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.PUT),
            beanClass = UsersHandler::class,
            beanMethod = "update",
            operation = Operation(
                operationId = "update",
                method = "PUT",
                security = [SecurityRequirement(name = "Keycloak access-token")],
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = UserUpdateDTO::class)
                        )
                    )
                ),
                parameters = arrayOf(
                    Parameter(
                        name = "id",
                        `in` = ParameterIn.PATH,
                        style = ParameterStyle.SIMPLE,
                        explode = Explode.FALSE,
                        required = true
                    )
                ),
            ),
            consumes = arrayOf("application/json"),
            produces = arrayOf("application/json")
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.PATCH),
            beanClass = UsersHandler::class,
            beanMethod = "updatePassword",
            operation = Operation(
                operationId = "updatePassword",
                method = "PATCH",
                security = [SecurityRequirement(name = "Keycloak access-token")],
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = UserPartialUpdateDTO::class)
                        )
                    )
                ),
                parameters = arrayOf(
                    Parameter(
                        name = "id",
                        `in` = ParameterIn.PATH,
                        style = ParameterStyle.SIMPLE,
                        explode = Explode.FALSE,
                        required = true
                    )
                ),
            ),
            consumes = arrayOf("application/json"),
            produces = arrayOf("application/json")
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.DELETE),
            beanClass = UsersHandler::class,
            beanMethod = "delete",
            operation = Operation(
                operationId = "delete",
                method = "DELETE",
                security = [SecurityRequirement(name = "Keycloak access-token")],
                parameters = [Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            ),
            produces = arrayOf("application/json")
        ),
    )
    @Bean
    fun usersRoutes(handler: UsersHandler): RouterFunction<ServerResponse> =
        RouterFunctions.route(RequestPredicates.POST("/users")) {
            handler.create(it).flatMap { u -> ServerResponse
                .created(UriComponentsBuilder.fromUriString("/users/${u.id}").build().toUri())
                .bodyValue(u)
            }
        }
            .andRoute(RequestPredicates.GET("/users")) {
                handler.list(it).collectList().flatMap { res -> ServerResponse.ok().bodyValue(res) }
            }
            .andRoute(RequestPredicates.GET("/users/{id}")) {
                handler.get(it).flatMap { u -> ServerResponse.ok().bodyValue(u) }
                    .switchIfEmpty(ServerResponse.notFound().build())
            }
            .andRoute(RequestPredicates.PUT("/users/{id}")) {
                handler.update(it).flatMap { u -> ServerResponse.ok().bodyValue(u) }
                    .switchIfEmpty(ServerResponse.notFound().build())
            }
            .andRoute(RequestPredicates.PATCH("/users/{id}")) {
                handler.get(it).flatMap { _ -> handler.updatePassword(it) }
                    .flatMap { u -> ServerResponse.ok().bodyValue(u) }
                    .switchIfEmpty(ServerResponse.notFound().build())
            }
            .andRoute(RequestPredicates.DELETE("/users/{id}")) {
                handler.get(it).flatMap { _ -> handler.delete(it) }
                    .flatMap { ServerResponse.noContent().build() }
                    .switchIfEmpty(ServerResponse.notFound().build())
            }
}

@Component
class UsersHandler(private val service: UsersService) {
    fun create(request: ServerRequest) =
        request.bodyToMono(UserCreateDTO::class.java).flatMap { u -> service.create(u) }

    fun list(request: ServerRequest) = service.list()

    fun get(request: ServerRequest) = service.get(request.pathVariable("id"))

    fun update(request: ServerRequest) =
        request.bodyToMono(UserUpdateDTO::class.java).flatMap { dto ->
            service.update(request.pathVariable("id"), dto)
        }

    fun updatePassword(request: ServerRequest) =
        request.bodyToMono(UserPartialUpdateDTO::class.java)
            .flatMap { service.updatePassword(request.pathVariable("id"), it) }

    fun delete(request: ServerRequest) =
        service.delete(request.pathVariable("id"))
}

@Service
class UsersService(private val keycloakClient: KeycloakClient) {
    fun create(user: UserCreateDTO): Mono<UserDTO> = keycloakClient.create(user)
    fun list(): Flux<UserDTO> = keycloakClient.list()
    fun get(id: String): Mono<UserDTO> = keycloakClient.get(id)
    fun update(id: String, dto: UserUpdateDTO) = keycloakClient.update(id, dto)
    fun updatePassword(id: String, dto: UserPartialUpdateDTO) = keycloakClient.updatePassword(id, dto)
    fun delete(id: String) = keycloakClient.delete(id)
}

