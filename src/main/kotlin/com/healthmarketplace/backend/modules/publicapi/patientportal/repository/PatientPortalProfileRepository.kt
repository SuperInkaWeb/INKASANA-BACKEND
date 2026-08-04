package com.healthmarketplace.backend.modules.publicapi.patientportal.repository

import com.healthmarketplace.backend.modules.publicapi.patientportal.entity.PatientPortalProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PatientPortalProfileRepository : JpaRepository<PatientPortalProfile, UUID> {
    fun findByEmail(email: String): Optional<PatientPortalProfile>
}