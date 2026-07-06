package com.healthmarketplace.backend.modules.tenant.user.entity

import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tenant_users")
class TenantUser(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "auth0_id", length = 180, unique = true)
    var auth0Id: String? = null,

    @Column(nullable = false, unique = true, length = 180)
    var email: String,

    @Column(name = "full_name", nullable = false, length = 180)
    var fullName: String,

    @Column(length = 50)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var role: TenantUserRole,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: TenantUserStatus = TenantUserStatus.ACTIVE,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,

    @Column(name = "last_login")
    var lastLogin: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)