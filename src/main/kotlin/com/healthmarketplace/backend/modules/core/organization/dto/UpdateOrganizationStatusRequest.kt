package com.healthmarketplace.backend.modules.core.organization.dto

import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus

data class UpdateOrganizationStatusRequest(
    val status: OrganizationStatus,
    val reason: String? = null
)