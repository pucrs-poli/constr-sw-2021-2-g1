package com.pucrs.construcaosoftware.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springdoc.core.annotations.RouterOperation
import org.springdoc.core.annotations.RouterOperations
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.ws.rs.NotFoundException

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
                requestBody = RequestBody(
                    required = true,
                    content = arrayOf(
                        Content(
                            schema = Schema(implementation = UserUpdateDTO::class)
                        )
                    )
                )
            ),
            consumes = arrayOf("application/json"),
            produces = arrayOf("application/json")
        ),
        RouterOperation(
            path = "/users",
            method = arrayOf(RequestMethod.GET),
            beanClass = UsersHandler::class,
            beanMethod = "list",
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
    fun routes(usersService: UsersService, handler: UsersHandler): RouterFunction<ServerResponse> =
        RouterFunctions.route(RequestPredicates.POST("/users")) {
            handler.create(it).flatMap { u -> ServerResponse.ok().bodyValue(u) }
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

@Component
class KeycloakClient(
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.roles}") private val roles: List<String>,
) {

    fun create(user: UserCreateDTO): Mono<UserDTO> {
        val realmResource = keycloak.realm(realm)

        val roleRepresentation = realmResource.roles().get(user.role).toRepresentation()

        val cred = CredentialRepresentation()
        cred.type = CredentialRepresentation.PASSWORD
        cred.isTemporary = false
        cred.value = user.password

        val userRepresentation = UserRepresentation()
        userRepresentation.isEnabled = true
        userRepresentation.username = user.username
        userRepresentation.email = user.email

        val response = keycloak.realm(realm).users().create(userRepresentation)
        val userId = CreatedResponseUtil.getCreatedId(response)

        val userResource = realmResource.users().get(userId)
        userResource.resetPassword(cred)
        userResource.roles().realmLevel().add(listOf(roleRepresentation))

        return Mono.just(UserDTO(userId, user.username, user.role, user.email))
    }

    fun list(): Flux<UserDTO> = Flux.fromIterable(
        roles.flatMap { role ->
            keycloak.realm(realm).roles().get(role).roleUserMembers.map {
                UserDTO(it.id, it.username, role, it.email)
            }
        }
    )

    fun get(id: String): Mono<UserDTO> {
        val user = keycloak.realm(realm).users().get(id)

        return Mono.fromCallable {
            val userRepresentation = user.toRepresentation()
            val role = user.roles().realmLevel().listAll().filter { roles.contains(it.name) }.firstOrNull()
                UserDTO(
                    userRepresentation.id,
                    userRepresentation.username,
                    role?.name,
                    userRepresentation.email
                )
            }
            .onErrorResume (NotFoundException::class.java) { Mono.empty() }
    }

    fun update(id: String, dto: UserUpdateDTO): Mono<UserDTO> {
        val realmResource = keycloak.realm(realm)
        val userResource = realmResource.users().get(id)

        val roleToAdd = realmResource.roles().get(dto.role).toRepresentation()
        val userRepresentation = UserRepresentation()
        userRepresentation.email = dto.email

        userResource.update(userRepresentation)

        val realmLevelRoles = userResource.roles().realmLevel()
        val rolesToRemove = realmLevelRoles.listAll().filter { roles.contains(it.name) }
        realmLevelRoles.remove(rolesToRemove)
        realmLevelRoles.add(listOf(roleToAdd))
        val currentRole = realmLevelRoles.listAll().filter { roles.contains(it.name) }.firstOrNull()

        val userR = userResource.toRepresentation()
        return Mono.just(UserDTO(userR.id, userR.username, currentRole?.name, userR.email))
    }

    fun updatePassword(id: String, user: UserPartialUpdateDTO): Mono<SuccessDTO> {
        val userResource = keycloak.realm(realm).users().get(id)

        val cred = CredentialRepresentation()
        cred.type = CredentialRepresentation.PASSWORD
        cred.isTemporary = false
        cred.value = user.password

        userResource.resetPassword(cred)

        return Mono.just(SuccessDTO(true))
    }

    fun delete(id: String): Mono<SuccessDTO> =
        Mono.fromCallable { keycloak.realm(realm).users().delete(id) }.then(Mono.just(SuccessDTO(true)))
}

data class UserDTO(
    val id: String,
    val username: String,
    val role: String? = null,
    val email: String? = null,
)

data class UserCreateDTO(
    val username: String,
    val role: String? = null,
    val email: String? = null,
    val password: String? = null
)

data class UserUpdateDTO(
    val role: String? = null,
    val email: String? = null,
)

data class UserPartialUpdateDTO(val password: String)

data class SuccessDTO(val success: Boolean? = false)