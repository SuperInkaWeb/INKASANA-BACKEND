package com.healthmarketplace.backend.modules.tenant.branding.dto

data class TenantBrandingRequest(

    val clinicName: String,

    val slogan: String? = null,

    val primaryColor: String? = "#1677ff",

    val secondaryColor: String? = "#001529",

    val logoUrl: String? = null,

    val faviconUrl: String? = null,

    val contactEmail: String? = null,

    val contactPhone: String? = null,

    val address: String? = null,

    val city: String? = null,

    val country: String? = null,

    val onboardingCompleted: Boolean? = false
)