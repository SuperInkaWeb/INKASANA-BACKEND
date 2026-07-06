package com.healthmarketplace.backend.modules.tenant.patient.repository

import com.healthmarketplace.backend.modules.tenant.patient.entity.Patient
import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PatientRepository : JpaRepository<Patient, UUID> {

    fun existsByIdentification(
        identification: String
    ): Boolean

    fun findByIdentification(
        identification: String
    ): Patient?

    fun findAllByStatus(
        status: PatientStatus
    ): List<Patient>

    fun findAllByStatusOrderByCreatedAtDesc(
        status: PatientStatus
    ): List<Patient>

    fun findAllByFullNameContainingIgnoreCaseOrIdentificationContainingIgnoreCaseOrEmailContainingIgnoreCase(
        fullName: String,
        identification: String,
        email: String
    ): List<Patient>

    fun findAllByOrderByCreatedAtDesc(): List<Patient>
}