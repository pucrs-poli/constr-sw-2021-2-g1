package com.pucrs.construcaosoftware.api

import javax.ws.rs.NotFoundException
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.resource.UsersResource
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
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.parameters.RequestBody

@Configuration
class UsersController {
    @RouterOperations(
        RouterOperation(path = "/users", method = arrayOf(RequestMethod.POST), beanClass = UsersService::class, beanMethod = "create"),
        RouterOperation(path = "/users", method = arrayOf(RequestMethod.GET), beanClass = UsersService::class, beanMethod = "list"),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.GET),
            beanClass = UsersService::class,
            beanMethod = "get",
            operation = Operation(
                operationId = "get",
                method = "GET",
                parameters=[Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            )
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.PUT),
            beanClass = UsersService::class,
            beanMethod = "update",
            operation = Operation(
                operationId = "update",
                method = "PUT",
                parameters=[Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            )
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.PATCH),
            beanClass = UsersService::class,
            beanMethod = "updatePassword",
            operation = Operation(
                operationId = "updatePassword",
                method = "PATCH",
                parameters=[Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            )
        ),
        RouterOperation(
            path = "/users/{id}",
            method = arrayOf(RequestMethod.DELETE),
            beanClass = UsersService::class,
            beanMethod = "delete",
            operation = Operation(
                operationId = "delete",
                method = "DELETE",
                parameters=[Parameter(
                    name = "id",
                    `in` = ParameterIn.PATH,
                    style = ParameterStyle.SIMPLE,
                    explode = Explode.FALSE,
                    required = true
                )]
            )
        ),
    )
    @Bean
    fun routes(usersService: UsersService): RouterFunction<ServerResponse> =
            RouterFunctions.route(RequestPredicates.POST("/users")) { it.bodyToMono(UserCreateDTO::class.java).flatMap { u -> usersService.create(u) }.flatMap { u -> ServerResponse.ok().bodyValue(u) } }
                    .andRoute(RequestPredicates.GET("/users")) { usersService.list().collectList().flatMap { ServerResponse.ok().bodyValue(it) } }
                    .andRoute(RequestPredicates.GET("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PUT("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> it.bodyToMono(UserUpdateDTO::class.java).flatMap { dto -> usersService.update(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PATCH("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> it.bodyToMono(UserPartialUpdateDTO::class.java).flatMap { dto -> usersService.updatePassword(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.DELETE("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> usersService.delete(u.id) }.flatMap { ServerResponse.noContent().build() }.switchIfEmpty(ServerResponse.notFound().build()) }
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
        roles.flatMap {
            role -> keycloak.realm(realm).roles().get(role).roleUserMembers.map {
                UserDTO(it.id, it.username, role, it.email)
            }
        }
    )
    
    fun get(id: String): Mono<UserDTO> {
        val user = keycloak.realm(realm).users().get(id)
        try {
            val userRepresentation = user.toRepresentation()
            val role = user.roles().realmLevel().listAll().filter {roles.contains(it.name)}.firstOrNull()
            return Mono.just(UserDTO(userRepresentation.id, userRepresentation.username, role?.name, userRepresentation.email))
        } catch (e: NotFoundException) {
            return Mono.empty()
        }
    }

    fun update(id: String, dto: UserUpdateDTO): Mono<UserDTO> {
        val realmResource = keycloak.realm(realm)
        val userResource = realmResource.users().get(id)

        val roleToAdd = realmResource.roles().get(dto.role).toRepresentation()
        val userRepresentation = UserRepresentation()
        userRepresentation.email = dto.email

        userResource.update(userRepresentation)

        val realmLevelRoles = userResource.roles().realmLevel()
        val rolesToRemove = realmLevelRoles.listAll().filter {roles.contains(it.name)}
        realmLevelRoles.remove(rolesToRemove)
        realmLevelRoles.add(listOf(roleToAdd))
        val currentRole = realmLevelRoles.listAll().filter {roles.contains(it.name)}.firstOrNull()

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

    fun delete(id: String): Mono<SuccessDTO> = Mono.fromCallable { keycloak.realm(realm).users().delete(id) }.then(Mono.just(SuccessDTO(true)))
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

data class SuccessDTO (val success: Boolean? = false)