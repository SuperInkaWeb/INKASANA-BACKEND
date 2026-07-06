package com.healthmarketplace.backend.config.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class JwtAuthConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val namespace = "https://medical-marketplace-api/"

        val auth0Roles =
            jwt.claims["${namespace}roles"] as? List<*> ?: emptyList<Any>()

        val internalRole =
            jwt.getClaimAsString("role")

        val roles = mutableSetOf<String>()

        auth0Roles
            .mapNotNull { it?.toString() }
            .forEach { roles.add(it) }

        if (!internalRole.isNullOrBlank()) {
            roles.add(internalRole)
        }

        val authorities = roles.map { role ->
            SimpleGrantedAuthority("ROLE_${role.uppercase()}")
        }

        return JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }
}