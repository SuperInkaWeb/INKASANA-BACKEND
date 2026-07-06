package com.healthmarketplace.backend.modules.tenant.doctor.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "doctor_specialties",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_doctor_specialty",
            columnNames = ["doctor_id", "specialty_id"]
        )
    ]
)
class DoctorSpecialty(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "doctor_id", nullable = false)
    var doctorId: UUID,

    @Column(name = "specialty_id", nullable = false)
    var specialtyId: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)