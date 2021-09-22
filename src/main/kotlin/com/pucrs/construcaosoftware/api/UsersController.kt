package com.pucrs.construcaosoftware.api

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
                    .andRoute(RequestPredicates.GET("/users/{id}")) { usersService.get(it.pathVariable("id").toLong()).flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PUT("/users/{id}")) { usersService.get(it.pathVariable("id").toLong()).flatMap { u -> it.bodyToMono(UserUpdateDTO::class.java).flatMap { dto -> usersService.update(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.PATCH("/users/{id}")) { usersService.get(it.pathVariable("id").toLong()).flatMap { u -> it.bodyToMono(UserPartialUpdateDTO::class.java).flatMap { dto -> usersService.updatePassword(u.id, dto) } }.flatMap { u -> ServerResponse.ok().bodyValue(u) }.switchIfEmpty(ServerResponse.notFound().build()) }
                    .andRoute(RequestPredicates.DELETE("/users/{id}")) { usersService.get(it.pathVariable("id").toLong()).flatMap { u -> usersService.delete(u.id) }.flatMap { ServerResponse.noContent().build() }.switchIfEmpty(ServerResponse.notFound().build()) }
}

@Service
class UsersService(private val keycloakClient: KeycloakClient) {
    fun create(user: UserDTO): Mono<Void> = keycloakClient.create(user)
    fun list(): Flux<UserDTO> = keycloakClient.list()
    fun get(id: Long): Mono<UserDTO> = keycloakClient.get(id)
    fun update(id: Long, dto: UserUpdateDTO) = keycloakClient.update(id, dto)
    fun updatePassword(id: Long, dto: UserPartialUpdateDTO) = keycloakClient.updatePassword(id, dto)
    fun delete(id: Long) = keycloakClient.delete(id)

}

@Component
class KeycloakClient {
    fun create(user: UserDTO): Mono<Void> =
            Mono.empty()

    fun list(): Flux<UserDTO> =
            Flux.empty()

    fun get(id: Long): Mono<UserDTO> = Mono.empty()

    fun update(id: Long, dto: UserUpdateDTO): Mono<UserDTO> = Mono.empty()

    fun updatePassword(id: Long, dto: UserPartialUpdateDTO): Mono<UserDTO> = Mono.empty()

    fun delete(id: Long): Mono<Void> = Mono.empty()
}

data class UserDTO(val id: Long, val username: String, val password: String)

data class UserUpdateDTO(val username: String, val password: String)

data class UserPartialUpdateDTO(val password: String)