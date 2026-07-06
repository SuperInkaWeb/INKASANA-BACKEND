package com.healthmarketplace.backend.modules.tenant.doctor.repository

import com.healthmarketplace.backend.modules.tenant.doctor.entity.Doctor
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface DoctorRepository : JpaRepository<Doctor, UUID> {

    fun findByLicenseNumber(licenseNumber: String): Optional<Doctor>

    fun existsByLicenseNumber(licenseNumber: String): Boolean

    fun existsByLicenseNumberAndIdNot(
        licenseNumber: String,
        id: UUID
    ): Boolean

    fun findAllByStatus(status: DoctorStatus): List<Doctor>

    fun findAllByStatusOrderByCreatedAtDesc(
        status: DoctorStatus
    ): List<Doctor>

    fun findAllByOrderByCreatedAtDesc(): List<Doctor>

    fun findAllBySpecialtyContainingIgnoreCase(
        specialty: String
    ): List<Doctor>

    fun findAllByFullNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCaseOrEmailContainingIgnoreCase(
        fullName: String,
        specialty: String,
        email: String
    ): List<Doctor>

    fun findByTenantUserId(tenantUserId: UUID): Optional<Doctor>
}