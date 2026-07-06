package com.healthmarketplace.backend.modules.tenant.doctor.repository

import com.healthmarketplace.backend.modules.tenant.doctor.entity.DoctorSpecialty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface DoctorSpecialtyRepository :
    JpaRepository<DoctorSpecialty, UUID> {

    fun findAllByDoctorId(
        doctorId: UUID
    ): List<DoctorSpecialty>

    @Modifying
    @Transactional
    fun deleteAllByDoctorId(
        doctorId: UUID
    )
}