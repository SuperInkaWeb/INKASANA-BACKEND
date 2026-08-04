package com.healthmarketplace.backend.modules.tenant.appointment.entity

import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "appointments")
class Appointment(
    @Id @GeneratedValue var id: UUID? = null,
    @Column(name = "patient_id", nullable = false) var patientId: UUID,
    @Column(name = "doctor_id", nullable = false) var doctorId: UUID,
    @Column(name = "tenant_id", nullable = false, length = 120) var tenantId: String,
    @Column(name = "appointment_date", nullable = false) var date: LocalDate,
    @Column(name = "appointment_time", nullable = false) var time: LocalTime,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    var status: AppointmentStatus = AppointmentStatus.PENDING,
    @Column(name = "reason", columnDefinition = "TEXT") var reason: String? = null,
    @Column(precision = 10, scale = 2) var price: BigDecimal? = null,
    @Column(name = "created_at", nullable = false) var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: LocalDateTime = LocalDateTime.now()
)