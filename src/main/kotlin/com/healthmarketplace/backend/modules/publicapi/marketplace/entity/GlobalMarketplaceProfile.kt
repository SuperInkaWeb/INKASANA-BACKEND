package com.healthmarketplace.backend.modules.publicapi.marketplace.entity

import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "marketplace_profiles_global", schema = "public")
class GlobalMarketplaceProfile(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "source_profile_id")
    var sourceProfileId: UUID? = null,

    @Column(name = "source_doctor_id")
    var sourceDoctorId: UUID? = null,

    @Column(name = "source_organization_id")
    var sourceOrganizationId: UUID? = null,

    @Column(name = "tenant_slug", nullable = false)
    var tenantSlug: String,

    @Column(name = "schema_name", nullable = false)
    var schemaName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false)
    var profileType: MarketplaceProfileType,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(name = "slug", nullable = false, unique = true)
    var slug: String,

    @Column(name = "headline")
    var headline: String? = null,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "city")
    var city: String? = null,

    @Column(name = "country")
    var country: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "email")
    var email: String? = null,

    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,

    @Column(name = "cover_image_url")
    var coverImageUrl: String? = null,

    @Column(name = "carousel_image_url_1")
    var carouselImageUrl1: String? = null,

    @Column(name = "carousel_image_url_2")
    var carouselImageUrl2: String? = null,

    @Column(name = "page_color")
    var pageColor: String? = null,

    @Column(name = "button_color")
    var buttonColor: String? = null,

    @Column(name = "subscription_color")
    var subscriptionColor: String? = null,

    @Column(name = "appearance_config", columnDefinition = "TEXT")
    var appearanceConfig: String? = null,

    @Column(name = "consultation_price")
    var consultationPrice: BigDecimal? = null,

    @Column(name = "consultation_duration_minutes")
    var consultationDurationMinutes: Int? = null,

    @Column(name = "available_days")
    var availableDays: String? = null,

    @Column(name = "available_start_time")
    var availableStartTime: String? = null,

    @Column(name = "available_end_time")
    var availableEndTime: String? = null,

    // JSON con los nombres de las especialidades del doctor, ej: ["Cardiología","Pediatría"]
    @Column(name = "specialties", columnDefinition = "TEXT")
    var specialties: String? = null,

    @Column(name = "is_published", nullable = false)
    var isPublished: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: MarketplaceProfileStatus = MarketplaceProfileStatus.PUBLISHED,

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
