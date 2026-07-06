package com.healthmarketplace.backend.modules.core.specialty.mapper

import com.healthmarketplace.backend.modules.core.specialty.dto.GlobalSpecialtyResponse
import com.healthmarketplace.backend.modules.core.specialty.entity.GlobalSpecialty

fun GlobalSpecialty.toResponse(): GlobalSpecialtyResponse {
    return GlobalSpecialtyResponse(
        id = this.id!!,
        name = this.name,
        slug = this.slug,
        description = this.description,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}