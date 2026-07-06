package com.healthmarketplace.backend.modules.core.organization.dto

import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationType

data class CreateOrganizationRequest(
    val name: String,
    val slug: String,
    val type: OrganizationType,
    val email: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val country: String?,
    val ownerEmail: String,
    val ownerFullName: String
)