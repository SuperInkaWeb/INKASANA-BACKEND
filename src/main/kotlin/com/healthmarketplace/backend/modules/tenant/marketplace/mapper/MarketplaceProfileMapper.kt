package com.healthmarketplace.backend.modules.tenant.marketplace.mapper

import com.healthmarketplace.backend.modules.tenant.marketplace.dto.MarketplaceProfileResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.entity.MarketplaceProfile

fun MarketplaceProfile.toResponse() =
    MarketplaceProfileResponse(
        id = id!!,
        profileType = profileType,
        doctorId = doctorId,
        organizationId = organizationId,
        displayName = displayName,
        slug = slug,
        headline = headline,
        description = description,
        city = city,
        country = country,
        address = address,
        phone = phone,
        email = email,
        profileImageUrl = profileImageUrl,
        coverImageUrl = coverImageUrl,
        consultationPrice = consultationPrice,
        consultationDurationMinutes = consultationDurationMinutes,
        isPublished = isPublished,
        status = status,
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )