package com.healthmarketplace.backend.modules.publicapi.auth.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.config.security.JwtTokenService
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginRequest
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginResponse
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantLoginTokenResponse
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantOrganizationSessionDto
import com.healthmarketplace.backend.modules.publicapi.auth.dto.TenantUserSessionDto
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Service
class TenantAuthService(
    private val organizationRepository: OrganizationRepository,
    private val tenantUserRepository: TenantUserRepository,
    private val jwtTokenService: JwtTokenService,
    transactionManager: PlatformTransactionManager
) {

    // Se usa TransactionTemplate (transacción programática) en vez de
    // @Transactional para poder abrir la transacción DESPUÉS de haber
    // llamado a TenantContext.setTenant(...). Así el find + update + save
    // corren dentro de UNA sola sesión de Hibernate / UNA sola conexión,
    // en vez de dos conexiones separadas, que era la ventana donde se
    // colaba el error "relation tenant_users does not exist" cuando una
    // conexión anterior del pool quedaba con el search_path equivocado.
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun login(request: TenantLoginRequest): TenantLoginResponse {
        val slug = request.slug.trim().lowercase()
        val email = request.email.trim().lowercase()

        val organization = organizationRepository
            .findBySlugAndStatus(slug, OrganizationStatus.ACTIVE)
            .orElseThrow {
                BusinessException("Organización no encontrada o inactiva")
            }

        if (!organization.schemaReady) {
            throw BusinessException("La organización todavía no está lista para iniciar sesión")
        }

        return try {
            TenantContext.setTenant(organization.schemaName)

            val user = transactionTemplate.execute {
                val foundUser = tenantUserRepository
                    .findByEmailAndStatus(email, TenantUserStatus.ACTIVE)
                    .orElseThrow {
                        BusinessException("Usuario no encontrado o inactivo")
                    }

                foundUser.lastLogin = LocalDateTime.now()
                foundUser.updatedAt = LocalDateTime.now()
                tenantUserRepository.save(foundUser)
            }!!

            TenantLoginResponse(
                organizationId = organization.id,
                organizationName = organization.name,
                slug = organization.slug,
                schemaName = organization.schemaName,
                userId = user.id,
                email = user.email,
                fullName = user.fullName,
                role = user.role.name,
                status = user.status.name
            )

        } finally {
            TenantContext.clear()
        }
    }

    fun loginWithToken(request: TenantLoginRequest): TenantLoginTokenResponse {
        val login = login(request)

        val token = jwtTokenService.createTenantToken(
            userId = requireNotNull(login.userId) { "El usuario no tiene ID" },
            orgId = requireNotNull(login.organizationId) { "La organización no tiene ID" },
            email = login.email,
            role = login.role,
            schemaName = login.schemaName
        )

        return TenantLoginTokenResponse(
            accessToken = token,
            expiresIn = 3600,

            user = TenantUserSessionDto(
                id = requireNotNull(login.userId).toString(),
                email = login.email,
                fullName = login.fullName,
                role = login.role,
                status = login.status
            ),

            organization = TenantOrganizationSessionDto(
                id = requireNotNull(login.organizationId).toString(),
                name = login.organizationName,
                slug = login.slug,
                schemaName = login.schemaName
            )
        )
    }

    fun loginWithAuth0(
        slug: String,
        email: String,
        auth0Id: String
    ): TenantLoginTokenResponse {
        val normalizedSlug = slug.trim().lowercase()
        val normalizedEmail = email.trim().lowercase()
        val normalizedAuth0Id = auth0Id.trim()

        val organization = organizationRepository
            .findBySlugAndStatus(normalizedSlug, OrganizationStatus.ACTIVE)
            .orElseThrow {
                BusinessException("Organización no encontrada o inactiva")
            }

        if (!organization.schemaReady) {
            throw BusinessException("La organización todavía no está lista para iniciar sesión")
        }

        return try {
            TenantContext.setTenant(organization.schemaName)

            val user = transactionTemplate.execute {
                val foundUser = tenantUserRepository
                    .findByEmailAndStatus(normalizedEmail, TenantUserStatus.ACTIVE)
                    .orElseThrow {
                        BusinessException("Usuario no encontrado o inactivo")
                    }

                if (!foundUser.auth0Id.isNullOrBlank() && foundUser.auth0Id != normalizedAuth0Id) {
                    throw BusinessException("Este correo ya está vinculado a otra cuenta Auth0")
                }

                if (foundUser.auth0Id.isNullOrBlank()) {
                    foundUser.auth0Id = normalizedAuth0Id
                }

                foundUser.lastLogin = LocalDateTime.now()
                foundUser.updatedAt = LocalDateTime.now()
                tenantUserRepository.save(foundUser)
            }!!

            val token = jwtTokenService.createTenantToken(
                userId = requireNotNull(user.id) { "El usuario no tiene ID" },
                orgId = requireNotNull(organization.id) { "La organización no tiene ID" },
                email = user.email,
                role = user.role.name,
                schemaName = organization.schemaName
            )

            TenantLoginTokenResponse(
                accessToken = token,
                expiresIn = 3600,
                user = TenantUserSessionDto(
                    id = requireNotNull(user.id).toString(),
                    email = user.email,
                    fullName = user.fullName,
                    role = user.role.name,
                    status = user.status.name
                ),
                organization = TenantOrganizationSessionDto(
                    id = requireNotNull(organization.id).toString(),
                    name = organization.name,
                    slug = organization.slug,
                    schemaName = organization.schemaName
                )
            )

        } finally {
            TenantContext.clear()
        }
    }
}