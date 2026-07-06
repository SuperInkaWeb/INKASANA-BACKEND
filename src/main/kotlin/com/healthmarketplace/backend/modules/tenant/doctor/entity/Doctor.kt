package com.healthmarketplace.backend.modules.tenant.doctor.entity

import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorVerificationStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "doctors")
class Doctor(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "tenant_user_id")
    var tenantUserId: UUID? = null,

    @Column(name = "full_name", nullable = false, length = 180)
    var fullName: String,

    @Column(length = 120)
    var specialty: String? = null,

    @Column(name = "license_number", length = 80)
    var licenseNumber: String? = null,

    @Column(length = 180)
    var email: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: DoctorStatus = DoctorStatus.ACTIVE,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(name = "consultation_price", precision = 10, scale = 2)
    var consultationPrice: BigDecimal? = null,

    @Column(name = "consultation_duration_minutes")
    var consultationDurationMinutes: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    var verificationStatus: DoctorVerificationStatus = DoctorVerificationStatus.PENDING,

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null,

    @Column(name = "verified_by")
    var verifiedBy: UUID? = null,

    @Column(name = "rejection_reason", length = 500)
    var rejectionReason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)