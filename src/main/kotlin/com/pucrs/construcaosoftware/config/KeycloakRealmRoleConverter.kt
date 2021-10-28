package com.pucrs.construcaosoftware.config

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.core.convert.converter.Converter


class KeycloakRealmRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(source: Jwt): Collection<GrantedAuthority> {
        val realmAccess = source.claims["realm_access"] as? Map<String, Any>
        if (realmAccess == null) {
            return emptyList()
        }
        val roles = realmAccess.get("roles") as? List<String>
        if (roles == null) {
            return emptyList()
        }
        return roles.map { r -> SimpleGrantedAuthority("ROLE_${r}") }
    }
}