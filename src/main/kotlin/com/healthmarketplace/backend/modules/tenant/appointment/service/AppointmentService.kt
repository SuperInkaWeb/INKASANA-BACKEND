package com.healthmarketplace.backend.modules.tenant.appointment.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.tenant.appointment.dto.*
import com.healthmarketplace.backend.modules.tenant.appointment.entity.Appointment
import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import com.healthmarketplace.backend.modules.tenant.appointment.repository.AppointmentRepository
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.patient.repository.PatientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
class AppointmentService(
    private val appointments: AppointmentRepository,
    private val patients: PatientRepository,
    private val doctors: DoctorRepository
) {
    @Transactional(readOnly = true)
    fun summary(): AppointmentSummaryResponse = AppointmentSummaryResponse(
        confirmedAppointments = appointments.countByStatus(AppointmentStatus.CONFIRMED)
    )

    @Transactional
    fun create(request: CreateAppointmentRequest): AppointmentResponse {
        val patient = patients.findById(request.patientId).orElseThrow { BusinessException("Paciente no encontrado") }
        val doctor = doctors.findById(request.doctorId).orElseThrow { BusinessException("Doctor no encontrado") }
        if (request.date.isBefore(LocalDate.now()) || (request.date == LocalDate.now() && !request.time.isAfter(LocalTime.now()))) {
            throw BusinessException("No se puede reservar un horario pasado")
        }
        if (appointments.existsByDoctorIdAndDateAndTimeAndStatusNot(doctor.id!!, request.date, request.time, AppointmentStatus.CANCELLED)) {
            throw BusinessException("Ese horario ya fue reservado")
        }
        val now = LocalDateTime.now()
        return response(appointments.save(
            Appointment(
                patientId = patient.id!!, doctorId = doctor.id!!, tenantId = TenantContext.getTenant(),
                date = request.date, time = request.time, reason = request.reason?.trim()?.ifBlank { null },
                price = doctor.consultationPrice, createdAt = now, updatedAt = now
            )
        ), patient.fullName, doctor.fullName)
    }

    @Transactional(readOnly = true)
    fun findAll(patientId: UUID?, doctorId: UUID?, date: LocalDate?): List<AppointmentResponse> {
        val list = when {
            patientId != null -> appointments.findAllByPatientIdOrderByDateDescTimeDesc(patientId)
            doctorId != null -> appointments.findAllByDoctorIdOrderByDateAscTimeAsc(doctorId)
            date != null -> appointments.findAllByDateOrderByTimeAsc(date)
            else -> appointments.findAll().sortedWith(compareBy<Appointment> { it.date }.thenBy { it.time })
        }
        return toResponses(list)
    }

    @Transactional(readOnly = true)
    fun findAllForUser(
        patientId: UUID?,
        doctorId: UUID?,
        date: LocalDate?,
        currentUserId: UUID,
        currentRole: String?
    ): List<AppointmentResponse> {
        if (currentRole?.uppercase() != "DOCTOR") {
            return findAll(patientId, doctorId, date)
        }

        val currentDoctor = doctors.findByTenantUserId(currentUserId)
            .orElseThrow { BusinessException("Tu usuario no tiene un perfil de doctor asociado") }
        val list = appointments.findAllByDoctorIdOrderByDateAscTimeAsc(currentDoctor.id!!)
            .filter { date == null || it.date == date }

        return toResponses(list)
    }

    private fun toResponses(list: List<Appointment>): List<AppointmentResponse> {
        return list.map { appointment -> response(appointment,
            patients.findById(appointment.patientId).orElse(null)?.fullName ?: "Paciente eliminado",
            doctors.findById(appointment.doctorId).orElse(null)?.fullName ?: "Doctor eliminado") }
    }

    @Transactional
    fun changeStatus(id: UUID, request: UpdateAppointmentStatusRequest): AppointmentResponse {
        val appointment = appointments.findById(id).orElseThrow { BusinessException("Cita no encontrada") }
        if (appointment.status == AppointmentStatus.CANCELLED) throw BusinessException("La cita ya está cancelada")
        appointment.status = request.status
        appointment.updatedAt = LocalDateTime.now()
        return response(appointments.save(appointment), patientName(appointment), doctorName(appointment))
    }

    @Transactional fun cancel(id: UUID): AppointmentResponse = changeStatus(id, UpdateAppointmentStatusRequest(AppointmentStatus.CANCELLED))
    private fun patientName(a: Appointment) = patients.findById(a.patientId).orElse(null)?.fullName ?: "Paciente eliminado"
    private fun doctorName(a: Appointment) = doctors.findById(a.doctorId).orElse(null)?.fullName ?: "Doctor eliminado"
    private fun response(a: Appointment, patientName: String, doctorName: String) = AppointmentResponse(a.id!!, a.patientId, patientName, a.doctorId, doctorName, a.tenantId, a.date, a.time, a.status, a.reason, a.price, a.createdAt)
}
