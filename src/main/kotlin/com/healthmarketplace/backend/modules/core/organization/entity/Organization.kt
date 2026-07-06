package com.healthmarketplace.backend.modules.core.organization.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "organizations", schema = "public")
class Organization(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(nullable = false, unique = true, length = 120)
    var slug: String,

    @Column(name = "schema_name", nullable = false, unique = true, length = 120)
    var schemaName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var type: OrganizationType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OrganizationStatus = OrganizationStatus.ACTIVE,

    @Column(length = 180)
    var email: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Column(length = 255)
    var address: String? = null,

    @Column(length = 120)
    var city: String? = null,

    @Column(length = 120)
    var country: String? = null,

    @Column(name = "schema_ready", nullable = false)
    var schemaReady: Boolean = false,

    @Column(name = "schema_ready_at")
    var schemaReadyAt: LocalDateTime? = null,

    @Column(name = "provisioning_error")
    var provisioningError: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)