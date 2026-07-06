package com.healthmarketplace.backend.config.multitenancy

import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

@Service
class TenantResolverService(
    private val organizationRepository: OrganizationRepository
) {

    companion object {
        private const val DEFAULT_TENANT = TenantContext.DEFAULT_TENANT
    }

    fun resolveFromJwt(jwt: Jwt?): String {
        if (jwt == null) return DEFAULT_TENANT

        val schemaFromToken =
            jwt.getClaimAsString("schema_name")
                ?: jwt.getClaimAsString("schema")

        if (!schemaFromToken.isNullOrBlank()) {
            return resolveBySchema(schemaFromToken)
        }

        val slugFromToken =
            jwt.getClaimAsString("organization_slug")
                ?: jwt.getClaimAsString("org_slug")
                ?: jwt.getClaimAsString("slug")

        if (!slugFromToken.isNullOrBlank()) {
            return resolveBySlug(slugFromToken)
        }

        return DEFAULT_TENANT
    }

    fun resolveBySlug(slug: String?): String {
        if (slug.isNullOrBlank()) return DEFAULT_TENANT

        val organization = organizationRepository
            .findBySlugAndStatus(slug.trim(), OrganizationStatus.ACTIVE)
            .orElse(null)

        return organization?.schemaName ?: DEFAULT_TENANT
    }

    fun resolveBySchema(schemaName: String?): String {
        if (schemaName.isNullOrBlank()) return DEFAULT_TENANT

        if (schemaName == DEFAULT_TENANT) return DEFAULT_TENANT

        val organization = organizationRepository
            .findBySchemaNameAndStatus(schemaName.trim(), OrganizationStatus.ACTIVE)
            .orElse(null)

        return organization?.schemaName ?: DEFAULT_TENANT
    }
}