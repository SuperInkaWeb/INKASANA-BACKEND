package com.healthmarketplace.backend.modules.publicapi.doctorregistration.service

import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.core.organization.dto.CreateOrganizationRequest
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationType
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.healthmarketplace.backend.modules.core.organization.service.OrganizationService
import com.healthmarketplace.backend.modules.publicapi.doctorregistration.dto.RegisterIndependentDoctorRequest
import com.healthmarketplace.backend.modules.publicapi.doctorregistration.dto.RegisterIndependentDoctorResponse
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import org.springframework.stereotype.Service

@Service
class IndependentDoctorRegistrationService(
    private val organizationService: OrganizationService,
    private val organizationRepository: OrganizationRepository,
    private val tenantUserRepository: TenantUserRepository
) {

    fun register(request: RegisterIndependentDoctorRequest): RegisterIndependentDoctorResponse {
        val fullName = request.fullName.trim()
        val email = request.email.trim().lowercase()

        require(fullName.isNotBlank()) {
            "El nombre del doctor es obligatorio"
        }

        require(email.isNotBlank()) {
            "El email del doctor es obligatorio"
        }

        val organizationName = request.professionalName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Consultorio $fullName"

        val slug = generateSlug(organizationName)

        if (slug.isBlank()) {
            throw IllegalArgumentException("El nombre profesional no permite generar un slug válido")
        }

        if (organizationRepository.existsBySlug(slug)) {
            throw IllegalArgumentException("Ya existe un consultorio registrado con ese nombre profesional")
        }

        val schemaName = "tenant_${slug.replace("-", "_")}"

        if (organizationRepository.existsBySchemaName(schemaName)) {
            throw IllegalArgumentException("Ya existe un consultorio registrado con ese schema")
        }

        val created = organizationService.create(
            CreateOrganizationRequest(
                name = organizationName,
                slug = slug,
                type = OrganizationType.INDEPENDENT_DOCTOR,
                email = email,
                phone = request.phone?.trim(),
                address = request.address?.trim(),
                city = request.city?.trim(),
                country = request.country?.trim() ?: "Ecuador",
                ownerEmail = email,
                ownerFullName = fullName
            )
        )

        val owner = try {
            TenantContext.setTenant(created.schemaName)

            tenantUserRepository.findByEmail(email)
                .orElseThrow {
                    IllegalStateException("No se encontró el OWNER creado para el doctor independiente")
                }
        } finally {
            TenantContext.clear()
        }

        return RegisterIndependentDoctorResponse(
            organizationId = created.id!!,
            organizationName = created.name,
            slug = created.slug,
            schemaName = created.schemaName,
            userId = owner.id,
            email = email,
            fullName = owner.fullName,
            role = owner.role.name,
            status = owner.status.name
        )
    }

    private fun generateSlug(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace(Regex("[áàäâ]"), "a")
            .replace(Regex("[éèëê]"), "e")
            .replace(Regex("[íìïî]"), "i")
            .replace(Regex("[óòöô]"), "o")
            .replace(Regex("[úùüû]"), "u")
            .replace("ñ", "n")
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}