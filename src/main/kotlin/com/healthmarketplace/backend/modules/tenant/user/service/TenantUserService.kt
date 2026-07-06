package com.healthmarketplace.backend.modules.tenant.user.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.integration.auth0.Auth0ManagementService
import com.healthmarketplace.backend.modules.tenant.user.dto.CreateTenantUserRequest
import com.healthmarketplace.backend.modules.tenant.user.dto.TenantUserResponse
import com.healthmarketplace.backend.modules.tenant.user.dto.UpdateTenantUserRequest
import com.healthmarketplace.backend.modules.tenant.user.entity.TenantUser
import com.healthmarketplace.backend.modules.tenant.user.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class TenantUserService(
    private val tenantUserRepository: TenantUserRepository,
    private val auth0ManagementService: Auth0ManagementService
) {

    fun findAll(
        status: TenantUserStatus?,
        role: TenantUserRole?,
        search: String?
    ): List<TenantUserResponse> {
        val users = when {
            role != null && status != null -> {
                tenantUserRepository.findAllByRoleAndStatus(role, status)
            }

            role != null -> {
                tenantUserRepository.findAllByRole(role)
            }

            status != null -> {
                tenantUserRepository.findAllByStatus(status)
            }

            !search.isNullOrBlank() -> {
                val term = search.trim()
                tenantUserRepository
                    .findAllByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term)
            }

            else -> {
                tenantUserRepository.findAllByOrderByCreatedAtDesc()
            }
        }

        return users.map { it.toResponse() }
    }

    fun findById(id: UUID): TenantUserResponse {
        val user = tenantUserRepository.findById(id)
            .orElseThrow { BusinessException("Usuario tenant no encontrado") }

        return user.toResponse()
    }

    fun create(request: CreateTenantUserRequest): TenantUserResponse {
        val normalizedEmail = request.email.trim().lowercase()
        val fullName = request.fullName.trim()

        if (normalizedEmail.isBlank()) {
            throw BusinessException("El email no puede estar vacío")
        }

        if (fullName.isBlank()) {
            throw BusinessException("El nombre no puede estar vacío")
        }

        if (tenantUserRepository.existsByEmail(normalizedEmail)) {
            throw BusinessException("Ya existe un usuario con ese email")
        }

        val auth0Id = auth0ManagementService.createUser(
            email = normalizedEmail,
            fullName = fullName
        )

        val now = LocalDateTime.now()

        val user = TenantUser(
            auth0Id = auth0Id,
            email = normalizedEmail,
            fullName = fullName,
            phone = request.phone?.trim()?.ifBlank { null },
            role = request.role,
            status = TenantUserStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )

        return tenantUserRepository.save(user).toResponse()
    }

    fun update(id: UUID, request: UpdateTenantUserRequest): TenantUserResponse {
        val user = tenantUserRepository.findById(id)
            .orElseThrow { BusinessException("Usuario tenant no encontrado") }

        if (
            user.role == TenantUserRole.OWNER &&
            request.role != null &&
            request.role != TenantUserRole.OWNER
        ) {
            throw BusinessException("No se puede cambiar el rol de un OWNER")
        }

        request.fullName?.let {
            val fullName = it.trim()

            if (fullName.isBlank()) {
                throw BusinessException("El nombre no puede estar vacío")
            }

            user.fullName = fullName
        }

        request.phone?.let {
            user.phone = it.trim().ifBlank { null }
        }

        request.role?.let {
            user.role = it
        }

        request.profileImageUrl?.let {
            user.profileImageUrl = it.trim().ifBlank { null }
        }

        user.updatedAt = LocalDateTime.now()

        return tenantUserRepository.save(user).toResponse()
    }

    fun activate(id: UUID): TenantUserResponse {
        return changeStatus(id, TenantUserStatus.ACTIVE)
    }

    fun suspend(id: UUID): TenantUserResponse {
        return changeStatus(id, TenantUserStatus.SUSPENDED)
    }

    fun deactivate(id: UUID): TenantUserResponse {
        return changeStatus(id, TenantUserStatus.INACTIVE)
    }

    fun findMe(email: String): TenantUserResponse {
        val user = tenantUserRepository.findByEmail(email.trim().lowercase())
            .orElseThrow { BusinessException("Usuario autenticado no encontrado") }

        return user.toResponse()
    }

    private fun changeStatus(
        id: UUID,
        status: TenantUserStatus
    ): TenantUserResponse {
        val user = tenantUserRepository.findById(id)
            .orElseThrow { BusinessException("Usuario tenant no encontrado") }

        if (
            user.role == TenantUserRole.OWNER &&
            status != TenantUserStatus.ACTIVE
        ) {
            throw BusinessException("No se puede suspender o desactivar un OWNER")
        }

        user.status = status
        user.updatedAt = LocalDateTime.now()

        return tenantUserRepository.save(user).toResponse()
    }
}