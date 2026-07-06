package com.healthmarketplace.backend.modules.tenant.patient.entity

import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "patients")
class Patient(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "full_name", nullable = false, length = 180)
    var fullName: String,

    @Column(length = 80)
    var identification: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Column(length = 30)
    var gender: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Column(length = 180)
    var email: String? = null,

    @Column(length = 255)
    var address: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PatientStatus = PatientStatus.ACTIVE,

    @Column(name = "emergency_contact_name", length = 180)
    var emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone", length = 50)
    var emergencyContactPhone: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)