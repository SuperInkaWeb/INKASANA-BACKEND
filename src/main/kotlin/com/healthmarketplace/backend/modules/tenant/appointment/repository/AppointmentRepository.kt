package com.healthmarketplace.backend.modules.tenant.appointment.repository

import com.healthmarketplace.backend.modules.tenant.appointment.entity.Appointment
import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

interface AppointmentRepository : JpaRepository<Appointment, UUID> {
    fun findAllByPatientIdOrderByDateDescTimeDesc(patientId: UUID): List<Appointment>
    fun findAllByDoctorIdOrderByDateAscTimeAsc(doctorId: UUID): List<Appointment>
    fun findAllByDateOrderByTimeAsc(date: LocalDate): List<Appointment>
    fun existsByDoctorIdAndDateAndTimeAndStatusNot(doctorId: UUID, date: LocalDate, time: LocalTime, status: AppointmentStatus): Boolean
    fun findAllByDoctorIdAndDateBetweenAndStatusNot(doctorId: UUID, from: LocalDate, to: LocalDate, status: AppointmentStatus): List<Appointment>
}
