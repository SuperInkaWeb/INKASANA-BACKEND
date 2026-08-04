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
        carouselImageUrl1 = carouselImageUrl1,
        carouselImageUrl2 = carouselImageUrl2,
        pageColor = pageColor,
        buttonColor = buttonColor,
        subscriptionColor = subscriptionColor,
        appearanceConfig = appearanceConfig,
        consultationPrice = consultationPrice,
        consultationDurationMinutes = consultationDurationMinutes,
        isPublished = isPublished,
        status = status,
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
