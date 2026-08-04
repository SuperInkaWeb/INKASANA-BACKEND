package com.healthmarketplace.backend.modules.tenant.agenda.entity

import com.healthmarketplace.backend.modules.tenant.agenda.model.AvailabilityExceptionType
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "availability_exceptions")
class AvailabilityException(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "doctor_id", nullable = false)
    var doctorId: UUID,

    @Column(name = "exception_date", nullable = false)
    var exceptionDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: AvailabilityExceptionType,

    // Null cuando type = UNAVAILABLE (bloquea el día completo)
    @Column(name = "start_time")
    var startTime: LocalTime? = null,

    @Column(name = "end_time")
    var endTime: LocalTime? = null,

    @Column(length = 255)
    var reason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
