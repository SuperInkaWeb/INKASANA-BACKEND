package com.healthmarketplace.backend.modules.publicapi.patientportal.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.security.JwtTokenService
import com.healthmarketplace.backend.modules.publicapi.auth.dto.PatientLoginTokenResponse
import com.healthmarketplace.backend.modules.publicapi.auth.dto.PatientLoginUserResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.entity.PatientPortalProfile
import com.healthmarketplace.backend.modules.publicapi.patientportal.repository.PatientPortalProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PatientPortalAuthService(
    private val profiles: PatientPortalProfileRepository,
    private val jwtTokenService: JwtTokenService
) {

    @Transactional
    fun loginWithAuth0(
        email: String,
        auth0Id: String
    ): PatientLoginTokenResponse {
        val normalizedEmail = email.trim().lowercase()
        val normalizedAuth0Id = auth0Id.trim()

        var profile = profiles.findByEmail(normalizedEmail).orElse(null)

        if (profile == null) {
            profile = profiles.save(
                PatientPortalProfile(
                    email = normalizedEmail,
                    auth0Id = normalizedAuth0Id
                )
            )
        } else {
            if (profile.auth0Id != normalizedAuth0Id) {
                throw BusinessException(
                    "Este correo ya está vinculado a otra cuenta de Auth0"
                )
            }

            if (profile.status != "ACTIVE") {
                throw BusinessException("Tu cuenta de paciente está inactiva")
            }

            profile.updatedAt = LocalDateTime.now()
            profiles.save(profile)
        }

        val token = jwtTokenService.createPlatformToken(
            userId = requireNotNull(profile.id),
            email = profile.email
        )

        return PatientLoginTokenResponse(
            accessToken = token,
            user = PatientLoginUserResponse(
                id = profile.id.toString(),
                email = profile.email
            )
        )
    }
}