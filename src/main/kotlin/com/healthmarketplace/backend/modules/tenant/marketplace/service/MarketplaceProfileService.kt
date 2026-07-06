package com.healthmarketplace.backend.modules.tenant.marketplace.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorVerificationStatus
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.CreateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.MarketplaceProfileResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.UpdateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.entity.MarketplaceProfile
import com.healthmarketplace.backend.modules.tenant.marketplace.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import com.healthmarketplace.backend.modules.tenant.marketplace.repository.MarketplaceProfileRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class MarketplaceProfileService(
    private val marketplaceProfileRepository: MarketplaceProfileRepository,
    private val doctorRepository: DoctorRepository,
    private val marketplaceSyncService: MarketplaceSyncService
) {

    fun findAll(): List<MarketplaceProfileResponse> {
        return marketplaceProfileRepository
            .findAll()
            .map { it.toResponse() }
    }

    fun findById(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        return profile.toResponse()
    }

    fun create(
        request: CreateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {

        if (marketplaceProfileRepository.existsBySlug(request.slug)) {
            throw BusinessException("Ya existe un perfil con ese slug")
        }

        val profile = MarketplaceProfile(
            profileType = request.profileType,
            doctorId = request.doctorId,
            organizationId = request.organizationId,
            displayName = request.displayName,
            slug = request.slug.trim().lowercase(),
            headline = request.headline,
            description = request.description,
            city = request.city,
            country = request.country,
            address = request.address,
            phone = request.phone,
            email = request.email,
            profileImageUrl = request.profileImageUrl,
            coverImageUrl = request.coverImageUrl,
            consultationPrice = request.consultationPrice,
            consultationDurationMinutes = request.consultationDurationMinutes
        )

        return marketplaceProfileRepository
            .save(profile)
            .toResponse()
    }

    fun update(
        id: UUID,
        request: UpdateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {

        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        request.displayName?.let { profile.displayName = it }
        request.headline?.let { profile.headline = it }
        request.description?.let { profile.description = it }
        request.city?.let { profile.city = it }
        request.country?.let { profile.country = it }
        request.address?.let { profile.address = it }
        request.phone?.let { profile.phone = it }
        request.email?.let { profile.email = it }
        request.profileImageUrl?.let { profile.profileImageUrl = it }
        request.coverImageUrl?.let { profile.coverImageUrl = it }
        request.consultationPrice?.let { profile.consultationPrice = it }
        request.consultationDurationMinutes?.let {
            profile.consultationDurationMinutes = it
        }

        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        if (
            savedProfile.isPublished &&
            savedProfile.status == MarketplaceProfileStatus.PUBLISHED
        ) {
            marketplaceSyncService.syncPublishedProfile(savedProfile)
        }

        return savedProfile.toResponse()
    }

    fun hide(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        profile.isPublished = false
        profile.status = MarketplaceProfileStatus.HIDDEN
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.removePublishedProfile(savedProfile)

        return savedProfile.toResponse()
    }

    fun publish(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        if (profile.profileType == MarketplaceProfileType.DOCTOR) {
            val doctorId = profile.doctorId
                ?: throw BusinessException("El perfil de doctor no tiene doctorId asociado")

            val doctor = doctorRepository
                .findById(doctorId)
                .orElseThrow {
                    BusinessException("Doctor no encontrado")
                }

            if (doctor.status != DoctorStatus.ACTIVE) {
                throw BusinessException("Solo se puede publicar un doctor activo")
            }

            if (doctor.verificationStatus != DoctorVerificationStatus.APPROVED) {
                throw BusinessException("Solo se puede publicar un doctor aprobado profesionalmente")
            }
        }

        profile.isPublished = true
        profile.status = MarketplaceProfileStatus.PUBLISHED
        profile.publishedAt = LocalDateTime.now()
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.syncPublishedProfile(savedProfile)

        return savedProfile.toResponse()
    }
}