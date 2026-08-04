package com.healthmarketplace.backend.modules.tenant.agenda.repository

import com.healthmarketplace.backend.modules.tenant.agenda.entity.AvailabilityException
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface AvailabilityExceptionRepository : JpaRepository<AvailabilityException, UUID> {

    fun findAllByDoctorIdOrderByExceptionDateAsc(
        doctorId: UUID
    ): List<AvailabilityException>

    fun findAllByDoctorIdAndExceptionDateBetweenOrderByExceptionDateAsc(
        doctorId: UUID,
        from: LocalDate,
        to: LocalDate
    ): List<AvailabilityException>

    fun findAllByDoctorIdAndExceptionDate(
        doctorId: UUID,
        exceptionDate: LocalDate
    ): List<AvailabilityException>

    fun deleteAllByDoctorId(doctorId: UUID)
}