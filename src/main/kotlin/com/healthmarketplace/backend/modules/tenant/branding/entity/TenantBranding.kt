package com.healthmarketplace.backend.modules.tenant.branding.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tenant_branding")
class TenantBranding(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "clinic_name", nullable = false)
    var clinicName: String,

    @Column(name = "slogan")
    var slogan: String? = null,

    @Column(name = "primary_color", nullable = false)
    var primaryColor: String = "#1677ff",

    @Column(name = "secondary_color", nullable = false)
    var secondaryColor: String = "#001529",

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @Column(name = "favicon_url")
    var faviconUrl: String? = null,

    @Column(name = "contact_email")
    var contactEmail: String? = null,

    @Column(name = "contact_phone")
    var contactPhone: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "city")
    var city: String? = null,

    @Column(name = "country")
    var country: String? = null,

    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)