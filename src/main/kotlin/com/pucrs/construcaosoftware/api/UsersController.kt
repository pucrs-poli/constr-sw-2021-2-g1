package com.pucrs.construcaosoftware.api

import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Configuration
class UsersController {

    @Bean
    fun routes(usersService: UsersService): RouterFunction<ServerResponse> =
            RouterFunctions.route(RequestPredicates.POST("/users")) { it.bodyToMono(UserDTO::class.java).flatMap { u -> usersService.create(u) }.then(ServerResponse.ok().build()) }
                    .andRoute(RequestPredicates.GET("/users")) { usersService.list().collectList().flatMap { ServerResponse.ok().bodyValue(it) } }
                    .andRoute(RequestPredicates.GET("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PUT("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> it.bodyToMono(UserUpdateDTO::class.java).flatMap { dto -> usersService.update(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PATCH("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> it.bodyToMono(UserPartialUpdateDTO::class.java).flatMap { dto -> usersService.updatePassword(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.DELETE("/users/{id}")) { usersService.get(it.pathVariable("id")).flatMap { u -> usersService.delete(u.id) }.flatMap { ServerResponse.noContent().build() }.switchIfEmpty(ServerResponse.notFound().build()) }
}

@Service
class UsersService(private val keycloakClient: KeycloakClient) {
    fun create(user: UserDTO): Mono<Void> = keycloakClient.create(user)
    fun list(): Flux<UserDTO> = keycloakClient.list()
    fun get(id: String): Mono<UserDTO> = keycloakClient.get(id)
    fun update(id: String, dto: UserUpdateDTO) = keycloakClient.update(id, dto)
    fun updatePassword(id: String, dto: UserPartialUpdateDTO) = keycloakClient.updatePassword(id, dto)
    fun delete(id: String) = keycloakClient.delete(id)

}

@Component
class KeycloakClient(private val keycloak: Keycloak) {

    companion object {
        private const val realm = "Puc"
    }

    fun create(user: UserDTO): Mono<Void> =
            Mono.empty()

    fun list(): Flux<UserDTO> =
            Flux.fromIterable(keycloak.realm(realm).users().list())
                    .map { UserDTO(it.id, it.username, it.email) }


    fun get(id: String): Mono<UserDTO> = Mono.justOrEmpty(keycloak.realm(realm)
            .users()
            .list().firstOrNull { it.id == id })
            .map { UserDTO(id, it.username, it.email) }

    fun update(id: String, dto: UserUpdateDTO): Mono<UserDTO> =
            Mono.empty()

    fun updatePassword(id: String, dto: UserPartialUpdateDTO): Mono<UserDTO> = Mono.empty()

    fun delete(id: String): Mono<Void> = Mono.fromCallable { keycloak.realm(realm).users().delete(id) }.then()
}

data class UserDTO(val id: String, val username: String, val email: String, val password: String? = null)

data class UserUpdateDTO(val username: String, val password: String)

data class UserPartialUpdateDTO(val password: String)