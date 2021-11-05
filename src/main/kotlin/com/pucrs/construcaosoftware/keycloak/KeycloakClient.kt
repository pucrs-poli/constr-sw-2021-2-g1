package com.pucrs.construcaosoftware.keycloak

import com.pucrs.construcaosoftware.dto.*
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.ws.rs.NotFoundException

@Component
class KeycloakClient(
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm}") private val realm: String,
    @Value("\${keycloak.roles}") private val roles: List<String>,
    @Value("\${keycloak.base}") private val baseUrl: String,
    @Value("\${keycloak.client-id}") private val clientId: String,
    @Value("\${keycloak.client-secret}") private val clientSecret: String
) {

    fun login(
        username: String,
        password: String,
    ): Mono<TokenDTO> {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("grant_type", "password")
        formData.add("client_id", clientId)
        formData.add("client_secret", clientSecret)
        formData.add("username", username)
        formData.add("password", password)

        val webClient = WebClient.create(baseUrl)
        val spec = webClient.post()
        val bodySpec = spec.uri("/realms/${realm}/protocol/openid-connect/token")
        val headersSpec = bodySpec.body(BodyInserters.fromFormData(formData))
        val responseSpec = headersSpec.retrieve()

        return responseSpec.bodyToMono(TokenDTO::class.java)
    }

    fun refreshToken(refreshToken: String): Mono<TokenDTO> {
        val formData = LinkedMultiValueMap<String, String>()
        formData.add("grant_type", "refresh_token")
        formData.add("client_id", clientId)
        formData.add("client_secret", clientSecret)
        formData.add("refresh_token", refreshToken)

        val webClient = WebClient.create(baseUrl)
        val spec = webClient.post()
        val bodySpec = spec.uri("/realms/${realm}/protocol/openid-connect/token")
        val headersSpec = bodySpec.body(BodyInserters.fromFormData(formData))
        val responseSpec = headersSpec.retrieve()

        return responseSpec.bodyToMono(TokenDTO::class.java)
    }

    fun certs(): Mono<Any> {
        val webClient = WebClient.create(baseUrl)
        val spec = webClient.get()
        val bodySpec = spec.uri("/realms/${realm}/protocol/openid-connect/certs")
        val responseSpec = bodySpec.retrieve()

        return responseSpec.bodyToMono(Any::class.java)
    }

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