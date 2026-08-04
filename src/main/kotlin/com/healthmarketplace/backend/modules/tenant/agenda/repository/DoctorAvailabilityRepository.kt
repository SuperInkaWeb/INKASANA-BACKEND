package com.healthmarketplace.backend.modules.tenant.agenda.repository

import com.healthmarketplace.backend.modules.tenant.agenda.entity.DoctorAvailability
import org.springframework.data.jpa.repository.JpaRepository
import java.time.DayOfWeek
import java.util.UUID

interface DoctorAvailabilityRepository : JpaRepository<DoctorAvailability, UUID> {

    fun findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(
        doctorId: UUID
    ): List<DoctorAvailability>

    fun findAllByDoctorIdAndDayOfWeekOrderByStartTimeAsc(
        doctorId: UUID,
        dayOfWeek: DayOfWeek
    ): List<DoctorAvailability>

    // Usado para listar disponibilidad de varios doctores en una sola
    // consulta (evita N+1 en pantallas tipo calendario/agenda general).
    fun findAllByDoctorIdInOrderByDayOfWeekAscStartTimeAsc(
        doctorIds: List<UUID>
    ): List<DoctorAvailability>

    fun deleteAllByDoctorId(doctorId: UUID)
}