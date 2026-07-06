package com.healthmarketplace.backend.modules.publicapi.marketplace.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceClinicDetailResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceClinicSearchResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceDoctorDetailResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.dto.MarketplaceDoctorSearchResponse
import com.healthmarketplace.backend.modules.publicapi.marketplace.repository.GlobalMarketplaceProfileRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PublicMarketplaceService(
    private val globalMarketplaceProfileRepository: GlobalMarketplaceProfileRepository
) {

    fun searchDoctors(
        search: String?,
        city: String?,
        country: String?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): List<MarketplaceDoctorSearchResponse> {

        val doctors = globalMarketplaceProfileRepository
            .findAllByProfileTypeAndIsPublishedTrueAndStatus(
                profileType = MarketplaceProfileType.DOCTOR,
                status = MarketplaceProfileStatus.PUBLISHED
            )

        return doctors
            .filter { profile ->
                val matchesSearch =
                    search.isNullOrBlank() ||
                            profile.displayName.contains(search, ignoreCase = true) ||
                            (profile.headline?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.description?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.city?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.country?.contains(search, ignoreCase = true) ?: false)

                val matchesCity =
                    city.isNullOrBlank() ||
                            profile.city.equals(city, ignoreCase = true)

                val matchesCountry =
                    country.isNullOrBlank() ||
                            profile.country.equals(country, ignoreCase = true)

                val matchesMinPrice =
                    minPrice == null ||
                            (profile.consultationPrice != null &&
                                    profile.consultationPrice!! >= minPrice)

                val matchesMaxPrice =
                    maxPrice == null ||
                            (profile.consultationPrice != null &&
                                    profile.consultationPrice!! <= maxPrice)

                matchesSearch &&
                        matchesCity &&
                        matchesCountry &&
                        matchesMinPrice &&
                        matchesMaxPrice
            }
            .map { profile ->
                MarketplaceDoctorSearchResponse(
                    id = profile.id!!,
                    doctorId = profile.sourceDoctorId,
                    displayName = profile.displayName,
                    slug = profile.slug,
                    headline = profile.headline,
                    city = profile.city,
                    country = profile.country,
                    profileImageUrl = profile.profileImageUrl,
                    consultationPrice = profile.consultationPrice,
                    consultationDurationMinutes = profile.consultationDurationMinutes
                )
            }
    }

    fun getDoctorBySlug(
        slug: String
    ): MarketplaceDoctorDetailResponse {

        val normalizedSlug = slug.trim().lowercase()

        val profile = globalMarketplaceProfileRepository.findBySlug(normalizedSlug)
            ?: throw BusinessException("Doctor no encontrado en marketplace")

        if (
            profile.profileType != MarketplaceProfileType.DOCTOR ||
            !profile.isPublished ||
            profile.status != MarketplaceProfileStatus.PUBLISHED
        ) {
            throw BusinessException("Doctor no disponible públicamente")
        }

        return MarketplaceDoctorDetailResponse(
            id = profile.id!!,
            doctorId = profile.sourceDoctorId,
            displayName = profile.displayName,
            slug = profile.slug,
            headline = profile.headline,
            description = profile.description,
            city = profile.city,
            country = profile.country,
            address = profile.address,
            phone = profile.phone,
            email = profile.email,
            profileImageUrl = profile.profileImageUrl,
            coverImageUrl = profile.coverImageUrl,
            consultationPrice = profile.consultationPrice,
            consultationDurationMinutes = profile.consultationDurationMinutes
        )
    }

    fun searchClinics(
        search: String?,
        city: String?,
        country: String?
    ): List<MarketplaceClinicSearchResponse> {

        val clinics = globalMarketplaceProfileRepository
            .findAllByProfileTypeAndIsPublishedTrueAndStatus(
                profileType = MarketplaceProfileType.CLINIC,
                status = MarketplaceProfileStatus.PUBLISHED
            )

        return clinics
            .filter { profile ->
                val matchesSearch =
                    search.isNullOrBlank() ||
                            profile.displayName.contains(search, ignoreCase = true) ||
                            (profile.headline?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.description?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.city?.contains(search, ignoreCase = true) ?: false) ||
                            (profile.country?.contains(search, ignoreCase = true) ?: false)

                val matchesCity =
                    city.isNullOrBlank() ||
                            profile.city.equals(city, ignoreCase = true)

                val matchesCountry =
                    country.isNullOrBlank() ||
                            profile.country.equals(country, ignoreCase = true)

                matchesSearch && matchesCity && matchesCountry
            }
            .map { profile ->
                MarketplaceClinicSearchResponse(
                    id = profile.id!!,
                    organizationId = profile.sourceOrganizationId,
                    displayName = profile.displayName,
                    slug = profile.slug,
                    headline = profile.headline,
                    city = profile.city,
                    country = profile.country,
                    address = profile.address,
                    phone = profile.phone,
                    profileImageUrl = profile.profileImageUrl
                )
            }
    }

    fun getClinicBySlug(
        slug: String
    ): MarketplaceClinicDetailResponse {

        val normalizedSlug = slug.trim().lowercase()

        val profile = globalMarketplaceProfileRepository.findBySlug(normalizedSlug)
            ?: throw BusinessException("Clínica no encontrada en marketplace")

        if (
            profile.profileType != MarketplaceProfileType.CLINIC ||
            !profile.isPublished ||
            profile.status != MarketplaceProfileStatus.PUBLISHED
        ) {
            throw BusinessException("Clínica no disponible públicamente")
        }

        return MarketplaceClinicDetailResponse(
            id = profile.id!!,
            organizationId = profile.sourceOrganizationId,
            displayName = profile.displayName,
            slug = profile.slug,
            headline = profile.headline,
            description = profile.description,
            city = profile.city,
            country = profile.country,
            address = profile.address,
            phone = profile.phone,
            email = profile.email,
            profileImageUrl = profile.profileImageUrl,
            coverImageUrl = profile.coverImageUrl
        )
    }
}