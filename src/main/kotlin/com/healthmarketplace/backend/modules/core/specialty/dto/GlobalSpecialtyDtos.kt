package com.healthmarketplace.backend.modules.core.specialty.dto

import com.healthmarketplace.backend.modules.core.specialty.model.GlobalSpecialtyStatus
import java.time.LocalDateTime
import java.util.UUID

data class CreateGlobalSpecialtyRequest(
    val name: String,
    val description: String?
)

data class UpdateGlobalSpecialtyRequest(
    val name: String?,
    val description: String?,
    val status: GlobalSpecialtyStatus?
)

data class GlobalSpecialtyResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val status: GlobalSpecialtyStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)