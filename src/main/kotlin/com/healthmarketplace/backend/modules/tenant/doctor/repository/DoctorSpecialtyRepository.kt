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

    // Trae en una sola consulta las relaciones de TODOS los doctores
    // pasados, en vez de tener que llamar findAllByDoctorId() una vez
    // por cada doctor (lo que generaba N+1 queries en findAll()).
    fun findAllByDoctorIdIn(
        doctorIds: List<UUID>
    ): List<DoctorSpecialty>

    @Modifying
    @Transactional
    fun deleteAllByDoctorId(
        doctorId: UUID
    )
}