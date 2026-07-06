package com.healthmarketplace.backend.config.multitenancy

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TenantFilter(
    private val tenantResolverService: TenantResolverService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication

            val schemaName = if (authentication is JwtAuthenticationToken) {
                val jwt = authentication.token
                val scope = jwt.getClaimAsString("scope")

                if (scope == "TENANT") {
                    tenantResolverService.resolveFromJwt(jwt)
                } else {
                    TenantContext.DEFAULT_TENANT
                }
            } else {
                TenantContext.DEFAULT_TENANT
            }

            TenantContext.setTenant(schemaName)

            println("TENANT ACTIVO -> ${TenantContext.getTenant()}")

            filterChain.doFilter(request, response)

        } finally {
            TenantContext.clear()
        }
    }
}