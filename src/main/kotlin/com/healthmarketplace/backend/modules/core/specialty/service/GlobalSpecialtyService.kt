package com.healthmarketplace.backend.modules.core.specialty.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.core.specialty.dto.CreateGlobalSpecialtyRequest
import com.healthmarketplace.backend.modules.core.specialty.dto.GlobalSpecialtyResponse
import com.healthmarketplace.backend.modules.core.specialty.dto.UpdateGlobalSpecialtyRequest
import com.healthmarketplace.backend.modules.core.specialty.entity.GlobalSpecialty
import com.healthmarketplace.backend.modules.core.specialty.mapper.toResponse
import com.healthmarketplace.backend.modules.core.specialty.model.GlobalSpecialtyStatus
import com.healthmarketplace.backend.modules.core.specialty.repository.GlobalSpecialtyRepository
import org.springframework.stereotype.Service
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.UUID

@Service
class GlobalSpecialtyService(
    private val repository: GlobalSpecialtyRepository
) {

    fun findAll(
        status: GlobalSpecialtyStatus?,
        search: String?
    ): List<GlobalSpecialtyResponse> {

        val specialties = when {
            !search.isNullOrBlank() -> {
                repository.findAllByNameContainingIgnoreCaseOrderByNameAsc(
                    search.trim()
                )
            }

            status != null -> {
                repository.findAllByStatusOrderByNameAsc(status)
            }

            else -> {
                repository.findAllByOrderByNameAsc()
            }
        }

        return specialties.map { it.toResponse() }
    }

    fun findActive(): List<GlobalSpecialtyResponse> {
        return repository
            .findAllByStatusOrderByNameAsc(GlobalSpecialtyStatus.ACTIVE)
            .map { it.toResponse() }
    }

    fun findById(id: UUID): GlobalSpecialtyResponse {
        return getEntity(id).toResponse()
    }

    fun create(
        request: CreateGlobalSpecialtyRequest
    ): GlobalSpecialtyResponse {

        val name = request.name.trim()

        if (name.isBlank()) {
            throw BusinessException("El nombre de la especialidad es obligatorio")
        }

        val slug = normalizeSlug(name)

        if (repository.existsBySlug(slug)) {
            throw BusinessException("Ya existe una especialidad con ese nombre")
        }

        val now = LocalDateTime.now()

        val specialty = GlobalSpecialty(
            name = name,
            slug = slug,
            description = request.description?.trim()?.ifBlank { null },
            status = GlobalSpecialtyStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )

        return repository.save(specialty).toResponse()
    }

    fun update(
        id: UUID,
        request: UpdateGlobalSpecialtyRequest
    ): GlobalSpecialtyResponse {

        val specialty = getEntity(id)

        request.name?.let {
            val newName = it.trim()

            if (newName.isBlank()) {
                throw BusinessException("El nombre de la especialidad es obligatorio")
            }

            val newSlug = normalizeSlug(newName)

            val duplicated = repository.findBySlug(newSlug)
                .filter { existing -> existing.id != specialty.id }
                .isPresent

            if (duplicated) {
                throw BusinessException("Ya existe una especialidad con ese nombre")
            }

            specialty.name = newName
            specialty.slug = newSlug
        }

        request.description?.let {
            specialty.description = it.trim().ifBlank { null }
        }

        request.status?.let {
            specialty.status = it
        }

        specialty.updatedAt = LocalDateTime.now()

        return repository.save(specialty).toResponse()
    }

    fun activate(id: UUID): GlobalSpecialtyResponse {
        val specialty = getEntity(id)

        specialty.status = GlobalSpecialtyStatus.ACTIVE
        specialty.updatedAt = LocalDateTime.now()

        return repository.save(specialty).toResponse()
    }

    fun deactivate(id: UUID): GlobalSpecialtyResponse {
        val specialty = getEntity(id)

        specialty.status = GlobalSpecialtyStatus.INACTIVE
        specialty.updatedAt = LocalDateTime.now()

        return repository.save(specialty).toResponse()
    }

    private fun getEntity(id: UUID): GlobalSpecialty {
        return repository.findById(id)
            .orElseThrow { BusinessException("Especialidad no encontrada") }
    }

    private fun normalizeSlug(value: String): String {
        val normalized = Normalizer.normalize(
            value.lowercase().trim(),
            Normalizer.Form.NFD
        )

        return normalized
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
    }
}