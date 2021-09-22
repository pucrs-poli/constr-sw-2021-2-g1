package com.pucrs.construcaosoftware.config

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.reactive.function.client.WebClient


@EnableWebFluxSecurity
class SecurityConfig(@Value("\${keycloak.base}") private val baseUrl: String,
                     @Value("\${keycloak.client-id}") private val clientId: String,
                     @Value("\${keycloak.client-secret}") private val clientSecret: String,
                     @Value("\${keycloak.user}") private val user: String,
                     @Value("\${keycloak.pass}") private val pass: String,
                     @Value("\${keycloak.realm}") private val realm: String
                     ) {
    @Bean
    fun tokenAugmentingWebClient(clientRegistrationRepository: ReactiveClientRegistrationRepository?,
                                 authorizedClientRepository: ServerOAuth2AuthorizedClientRepository?): WebClient {
        return WebClient.builder()
                .filter(ServerOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository, authorizedClientRepository))
                .build()
    }

    @Bean
    fun keycloak() : Keycloak =
            KeycloakBuilder.builder()
                    .grantType(OAuth2Constants.PASSWORD)
                    .serverUrl(baseUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(user)
                    .password(pass)
                    .build()

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        // the matcher for all paths that need to be secured (require a logged-in user)
        val apiPathMatcher = ServerWebExchangeMatchers.pathMatchers(API_MATCHER_PATH)
        return http
                .authorizeExchange().matchers(apiPathMatcher).authenticated()
                .anyExchange().permitAll()
                .and().httpBasic().disable()
                .csrf().disable()
                .oauth2Client()
                .and()
                .oauth2Login()
                .and()
                .build()
    }

    companion object {
        const val API_MATCHER_PATH = "*"
    }
}