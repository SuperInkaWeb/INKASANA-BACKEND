package com.healthmarketplace.backend.modules.core.specialty.entity

import com.healthmarketplace.backend.modules.core.specialty.model.GlobalSpecialtyStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "global_specialties", schema = "public")
class GlobalSpecialty(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true, length = 120)
    var name: String,

    @Column(nullable = false, unique = true, length = 140)
    var slug: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: GlobalSpecialtyStatus = GlobalSpecialtyStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)