package com.healthmarketplace.backend.modules.publicapi.patientportal.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.publicapi.media.service.MediaFileService
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.PatientPortalProfileResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.PatientPortalAppointmentResponse
import com.healthmarketplace.backend.modules.publicapi.patientportal.dto.UpdatePatientPortalProfileRequest
import com.healthmarketplace.backend.modules.publicapi.patientportal.entity.PatientPortalProfile
import com.healthmarketplace.backend.modules.publicapi.patientportal.repository.PatientPortalProfileRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.util.UUID

@Service
class PatientPortalProfileService(
    private val profiles: PatientPortalProfileRepository,
    private val mediaFileService: MediaFileService,
    private val jdbcTemplate: JdbcTemplate
) {

    @Transactional(readOnly = true)
    fun appointments(jwt: Jwt): List<PatientPortalAppointmentResponse> {
        val profile = findProfile(jwt)
        return jdbcTemplate.query(
            """
            SELECT c.appointment_id, c.id AS checkout_id, c.appointment_date, c.appointment_time,
                   COALESCE(mp.display_name, 'Doctor') AS doctor_name, o.name AS clinic_name
            FROM appointment_payment_checkouts c
            JOIN organizations o ON o.id = c.organization_id
            LEFT JOIN marketplace_profiles_global mp
              ON mp.source_doctor_id = c.doctor_id AND mp.schema_name = c.tenant_schema AND mp.profile_type = 'DOCTOR'
            WHERE c.patient_portal_profile_id = ? AND c.status = 'APPROVED'
            ORDER BY c.appointment_date ASC, c.appointment_time ASC
            """.trimIndent(),
            { rs, _ -> PatientPortalAppointmentResponse(
                (rs.getObject("appointment_id") ?: rs.getObject("checkout_id")).toString(),
                rs.getString("doctor_name"), rs.getString("clinic_name"),
                rs.getObject("appointment_date", LocalDate::class.java),
                rs.getObject("appointment_time", LocalTime::class.java), "PAID"
            ) }, requireNotNull(profile.id)
        )
    }

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

        request.phone?.let {
            profile.phone = it.trim().ifBlank { null }
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
            phone = profile.phone,
            avatarUrl = profile.avatarUrl
        )
}
