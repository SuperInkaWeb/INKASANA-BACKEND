package com.healthmarketplace.backend.modules.publicapi.patientportal.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.publicapi.media.service.MediaFileService
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.PatientPortalProfileResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.UpdatePatientPortalProfileRequest
import com.healthmarketplace.backend.modules.publicapi.patientportal.entity.PatientPortalProfile
import com.healthmarketplace.backend.modules.publicapi.patientportal.repository.PatientPortalProfileRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDateTime
import java.util.UUID

@Service
class PatientPortalProfileService(
    private val profiles: PatientPortalProfileRepository,
    private val mediaFileService: MediaFileService
) {

    @Transactional(readOnly = true)
    fun getMe(jwt: Jwt): PatientPortalProfileResponse {
        return response(findProfile(jwt))
    }

    @Transactional
    fun updateMe(
        jwt: Jwt,
        request: UpdatePatientPortalProfileRequest
    ): PatientPortalProfileResponse {
        val profile = findProfile(jwt)

        request.firstName?.let {
            profile.firstName = it.trim().ifBlank { null }
        }

        request.lastName?.let {
            profile.lastName = it.trim().ifBlank { null }
        }

        request.dni?.let {
            profile.dni = it.trim().ifBlank { null }
        }

        profile.updatedAt = LocalDateTime.now()

        return response(profiles.save(profile))
    }

    @Transactional
    fun updateAvatar(
        jwt: Jwt,
        file: MultipartFile
    ): PatientPortalProfileResponse {
        val profile = findProfile(jwt)
        val mediaFile = mediaFileService.storeImage(file)

        profile.avatarUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/public/media/${mediaFile.id}")
            .toUriString()

        profile.updatedAt = LocalDateTime.now()

        return response(profiles.save(profile))
    }

    private fun findProfile(jwt: Jwt): PatientPortalProfile {
        val id = UUID.fromString(jwt.subject)

        return profiles.findById(id)
            .orElseThrow { BusinessException("Perfil de paciente no encontrado") }
    }

    private fun response(profile: PatientPortalProfile) =
        PatientPortalProfileResponse(
            id = requireNotNull(profile.id).toString(),
            email = profile.email,
            firstName = profile.firstName,
            lastName = profile.lastName,
            dni = profile.dni,
            avatarUrl = profile.avatarUrl
        )
}