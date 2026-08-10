package com.healthmarketplace.backend.modules.publicapi.patientportal.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "patient_portal_profiles", schema = "public")
class PatientPortalProfile(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(nullable = false, unique = true, length = 180)
    var email: String,

    @Column(name = "auth0_id", nullable = false, unique = true, length = 255)
    var auth0Id: String,

    @Column(name = "first_name", length = 100)
    var firstName: String? = null,

    @Column(name = "last_name", length = 100)
    var lastName: String? = null,

    @Column(length = 20)
    var dni: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Column(name = "avatar_url", length = 500)
    var avatarUrl: String? = null,

    @Column(nullable = false, length = 30)
    var status: String = "ACTIVE",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
