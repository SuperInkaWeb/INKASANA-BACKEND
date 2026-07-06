package com.healthmarketplace.backend.modules.core.organization.service

import com.healthmarketplace.backend.modules.core.organization.dto.CreateOrganizationRequest
import com.healthmarketplace.backend.modules.core.organization.dto.OrganizationResponse
import com.healthmarketplace.backend.modules.core.organization.dto.UpdateOrganizationStatusRequest
import com.healthmarketplace.backend.modules.core.organization.entity.Organization
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.healthmarketplace.backend.modules.core.tenant.service.TenantProvisioningService
import com.healthmarketplace.backend.modules.tenant.bootstrap.service.TenantBootstrapService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val tenantProvisioningService: TenantProvisioningService,
    private val tenantBootstrapService: TenantBootstrapService
) {

    fun create(request: CreateOrganizationRequest): OrganizationResponse {
        val cleanSlug = normalizeSlug(request.slug)
        val schemaName = "tenant_${cleanSlug.replace("-", "_")}"

        if (organizationRepository.existsBySlug(cleanSlug)) {
            throw IllegalArgumentException("Ya existe una organización con el slug: $cleanSlug")
        }

        if (organizationRepository.existsBySchemaName(schemaName)) {
            throw IllegalArgumentException("Ya existe una organización con el schema: $schemaName")
        }

        val now = LocalDateTime.now()

        val organization = Organization(
            name = request.name.trim(),
            slug = cleanSlug,
            schemaName = schemaName,
            type = request.type,
            status = OrganizationStatus.ACTIVE,
            email = request.email?.trim(),
            phone = request.phone?.trim(),
            address = request.address?.trim(),
            city = request.city?.trim(),
            country = request.country?.trim(),
            schemaReady = false,
            schemaReadyAt = null,
            provisioningError = null,
            createdAt = now,
            updatedAt = now
        )

        val saved = organizationRepository.save(organization)

        try {
            tenantProvisioningService.provision(schemaName)

            tenantBootstrapService.createOwner(
                schemaName = schemaName,
                ownerEmail = request.ownerEmail.trim(),
                ownerFullName = request.ownerFullName.trim()
            )

            saved.schemaReady = true
            saved.schemaReadyAt = LocalDateTime.now()
            saved.provisioningError = null
            saved.updatedAt = LocalDateTime.now()

            return organizationRepository.save(saved).toResponse()

        } catch (ex: Exception) {
            saved.schemaReady = false
            saved.schemaReadyAt = null
            saved.provisioningError = ex.message ?: "Error desconocido al provisionar tenant"
            saved.updatedAt = LocalDateTime.now()

            organizationRepository.save(saved)

            throw IllegalStateException(
                "Error al provisionar tenant: ${ex.message}",
                ex
            )
        }
    }

    fun findAll(): List<OrganizationResponse> {
        return organizationRepository.findAll()
            .map { it.toResponse() }
    }

    fun updateStatus(
        id: UUID,
        request: UpdateOrganizationStatusRequest
    ): OrganizationResponse {

        val organization = organizationRepository.findById(id)
            .orElseThrow {
                IllegalArgumentException("Organización no encontrada")
            }

        organization.status = request.status
        organization.updatedAt = LocalDateTime.now()

        return organizationRepository.save(organization).toResponse()
    }

    private fun normalizeSlug(slug: String): String {
        val clean = slug.trim().lowercase()

        require(clean.matches(Regex("^[a-z0-9-]+$"))) {
            "El slug solo puede contener letras minúsculas, números y guiones"
        }

        return clean
    }

    private fun Organization.toResponse(): OrganizationResponse {
        return OrganizationResponse(
            id = id,
            name = name,
            slug = slug,
            schemaName = schemaName,
            type = type,
            status = status,
            email = email,
            phone = phone,
            address = address,
            city = city,
            country = country,
            schemaReady = schemaReady,
            schemaReadyAt = schemaReadyAt,
            provisioningError = provisioningError,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}