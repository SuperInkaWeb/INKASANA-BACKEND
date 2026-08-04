package com.healthmarketplace.backend.modules.tenant.marketplace.service
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.config.multitenancy.TenantContext
import com.healthmarketplace.backend.modules.core.organization.entity.OrganizationStatus
import com.healthmarketplace.backend.modules.core.organization.repository.OrganizationRepository
import com.healthmarketplace.backend.modules.core.specialty.repository.GlobalSpecialtyRepository
import com.healthmarketplace.backend.modules.tenant.doctor.entity.Doctor
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorSpecialtyRepository
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorVerificationStatus
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.CreateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.MarketplaceProfileResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.dto.UpdateMarketplaceProfileRequest
import com.healthmarketplace.backend.modules.tenant.marketplace.entity.MarketplaceProfile
import com.healthmarketplace.backend.modules.tenant.marketplace.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileStatus
import com.healthmarketplace.backend.modules.tenant.marketplace.model.MarketplaceProfileType
import com.healthmarketplace.backend.modules.tenant.marketplace.repository.MarketplaceProfileRepository
import com.healthmarketplace.backend.modules.publicapi.marketplace.repository.GlobalMarketplaceProfileRepository
import com.healthmarketplace.backend.modules.publicapi.media.service.MediaFileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.UUID
import com.healthmarketplace.backend.modules.core.organization.entity.Organization

@Service
class MarketplaceProfileService(
    private val marketplaceProfileRepository: MarketplaceProfileRepository,
    private val doctorRepository: DoctorRepository,
    private val doctorSpecialtyRepository: DoctorSpecialtyRepository,
    private val globalSpecialtyRepository: GlobalSpecialtyRepository,
    private val organizationRepository: OrganizationRepository,
    private val marketplaceSyncService: MarketplaceSyncService,
    private val globalMarketplaceProfileRepository: GlobalMarketplaceProfileRepository,
    private val objectMapper: ObjectMapper,
    private val mediaFileService: MediaFileService
) {

    private fun resolveDoctorHeadline(doctor: Doctor): String? {
        val explicitSpecialty = doctor.specialty?.trim()?.ifBlank { null }

        if (explicitSpecialty != null) {
            return explicitSpecialty
        }

        val doctorId = doctor.id ?: return null

        val specialtyIds = doctorSpecialtyRepository
            .findAllByDoctorId(doctorId)
            .map { it.specialtyId }

        if (specialtyIds.isEmpty()) {
            return null
        }

        val names = globalSpecialtyRepository
            .findAllById(specialtyIds)
            .map { it.name }

        return names.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun buildSpecialtiesJson(doctor: Doctor): String {
        val doctorId = doctor.id ?: return objectMapper.writeValueAsString(emptyList<String>())

        val specialtyIds = doctorSpecialtyRepository
            .findAllByDoctorId(doctorId)
            .map { it.specialtyId }

        if (specialtyIds.isEmpty()) {
            return objectMapper.writeValueAsString(emptyList<String>())
        }

        val names = globalSpecialtyRepository
            .findAllById(specialtyIds)
            .map { it.name }

        return objectMapper.writeValueAsString(names)
    }

    fun findAll(): List<MarketplaceProfileResponse> {
        return marketplaceProfileRepository
            .findAll()
            .map { it.toResponse() }
    }

    fun findById(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        return profile.toResponse()
    }

    fun create(
        request: CreateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {

        if (marketplaceProfileRepository.existsBySlug(request.slug)) {
            throw BusinessException("Ya existe un perfil con ese slug")
        }

        val profile = MarketplaceProfile(
            profileType = request.profileType,
            doctorId = request.doctorId,
            organizationId = request.organizationId,
            displayName = request.displayName,
            slug = request.slug.trim().lowercase(),
            headline = request.headline,
            description = request.description,
            city = request.city,
            country = request.country,
            address = request.address,
            phone = request.phone,
            email = request.email,
            profileImageUrl = request.profileImageUrl,
            coverImageUrl = request.coverImageUrl,
            consultationPrice = request.consultationPrice,
            consultationDurationMinutes = request.consultationDurationMinutes
        )

        return marketplaceProfileRepository
            .save(profile)
            .toResponse()
    }

    /**
     * Devuelve el perfil de marketplace (tipo CLINIC) de la organización
     * activa del tenant actual. Se usa desde el dashboard para que el
     * dueño/admin de la clínica vea y edite su propia info pública
     * (ej. descripción de la clínica).
     */
    fun findMyOrganizationProfile(): MarketplaceProfileResponse {
        val organization = organizationRepository
            .findBySchemaNameAndStatus(TenantContext.getTenant(), OrganizationStatus.ACTIVE)
            .orElseThrow { BusinessException("No se encontró la organización activa") }

        val profile = marketplaceProfileRepository.findByOrganizationIdAndProfileType(organization.id!!, MarketplaceProfileType.CLINIC)
            ?: throw BusinessException(
                "Tu organización todavía no tiene un perfil de marketplace. " +
                        "Debe estar activa y aprobada primero."
            )

        return profile.toResponse()
    }

    /**
     * Actualiza el perfil de marketplace de la organización activa del
     * tenant actual (ej. descripción, teléfono, imágenes, etc.) y propaga
     * el cambio al marketplace público si el perfil ya está publicado.
     */
    fun updateMyOrganizationProfile(
        request: UpdateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {
        val organization = organizationRepository
            .findBySchemaNameAndStatus(TenantContext.getTenant(), OrganizationStatus.ACTIVE)
            .orElseThrow { BusinessException("No se encontró la organización activa") }

        val profile = marketplaceProfileRepository.findByOrganizationIdAndProfileType(organization.id!!, MarketplaceProfileType.CLINIC)
            ?: throw BusinessException(
                "Tu organización todavía no tiene un perfil de marketplace. " +
                        "Debe estar activa y aprobada primero."
            )

        return update(profile.id!!, request)
    }

    /**
     * Sube (arrastrada/soltada o por click) la foto de perfil o de portada
     * del perfil de marketplace de la organización activa del tenant actual,
     * y la guarda como profileImageUrl o coverImageUrl según corresponda.
     * Sigue el mismo patrón que DoctorService.updateProfilePhoto.
     */
    @Transactional
    fun uploadMyOrganizationImage(
        imageType: String,
        file: MultipartFile
    ): MarketplaceProfileResponse {
        val organization = organizationRepository
            .findBySchemaNameAndStatus(TenantContext.getTenant(), OrganizationStatus.ACTIVE)
            .orElseThrow { BusinessException("No se encontró la organización activa") }

        val profile = marketplaceProfileRepository.findByOrganizationIdAndProfileType(organization.id!!, MarketplaceProfileType.CLINIC)
            ?: throw BusinessException(
                "Tu organización todavía no tiene un perfil de marketplace. " +
                        "Debe estar activa y aprobada primero."
            )

        val mediaFile = mediaFileService.storeImage(file)

        val publicUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/public/media/${mediaFile.id}")
            .toUriString()

        when (imageType) {
            "profile" -> profile.profileImageUrl = publicUrl
            "cover" -> profile.coverImageUrl = publicUrl
            "carousel-1" -> profile.carouselImageUrl1 = publicUrl
            "carousel-2" -> profile.carouselImageUrl2 = publicUrl
            else -> throw BusinessException("Tipo de imagen inválido. Usa 'profile', 'cover', 'carousel-1' o 'carousel-2'")
        }

        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        if (
            savedProfile.isPublished &&
            savedProfile.status == MarketplaceProfileStatus.PUBLISHED
        ) {
            marketplaceSyncService.syncPublishedProfile(savedProfile)
        }

        return savedProfile.toResponse()
    }

    fun update(
        id: UUID,
        request: UpdateMarketplaceProfileRequest
    ): MarketplaceProfileResponse {

        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        request.displayName?.let { profile.displayName = it }
        request.headline?.let { profile.headline = it }
        request.description?.let { profile.description = it }
        request.city?.let { profile.city = it }
        request.country?.let { profile.country = it }
        request.address?.let { profile.address = it }
        request.phone?.let { profile.phone = it }
        request.email?.let { profile.email = it }
        request.profileImageUrl?.let { profile.profileImageUrl = it }
        request.coverImageUrl?.let { profile.coverImageUrl = it }
        request.carouselImageUrl1?.let { profile.carouselImageUrl1 = it }
        request.carouselImageUrl2?.let { profile.carouselImageUrl2 = it }
        request.pageColor?.let { profile.pageColor = it }
        request.buttonColor?.let { profile.buttonColor = it }
        request.subscriptionColor?.let { profile.subscriptionColor = it }
        request.appearanceConfig?.let { profile.appearanceConfig = it }
        request.consultationPrice?.let { profile.consultationPrice = it }
        request.consultationDurationMinutes?.let {
            profile.consultationDurationMinutes = it
        }

        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        if (
            savedProfile.isPublished &&
            savedProfile.status == MarketplaceProfileStatus.PUBLISHED
        ) {
            marketplaceSyncService.syncPublishedProfile(savedProfile)
        }

        return savedProfile.toResponse()
    }

    fun hide(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        profile.isPublished = false
        profile.status = MarketplaceProfileStatus.HIDDEN
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.removePublishedProfile(savedProfile)

        return savedProfile.toResponse()
    }

    fun publish(id: UUID): MarketplaceProfileResponse {
        val profile = marketplaceProfileRepository
            .findById(id)
            .orElseThrow {
                BusinessException("Perfil de marketplace no encontrado")
            }

        if (profile.profileType == MarketplaceProfileType.DOCTOR) {
            val doctorId = profile.doctorId
                ?: throw BusinessException("El perfil de doctor no tiene doctorId asociado")

            val doctor = doctorRepository
                .findById(doctorId)
                .orElseThrow {
                    BusinessException("Doctor no encontrado")
                }

            if (doctor.status != DoctorStatus.ACTIVE) {
                throw BusinessException("Solo se puede publicar un doctor activo")
            }

            if (doctor.verificationStatus != DoctorVerificationStatus.APPROVED) {
                throw BusinessException("Solo se puede publicar un doctor aprobado profesionalmente")
            }
        }

        profile.isPublished = true
        profile.status = MarketplaceProfileStatus.PUBLISHED
        profile.publishedAt = LocalDateTime.now()
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.syncPublishedProfile(savedProfile)

        return savedProfile.toResponse()
    }
    /**
     * Crea (o actualiza) y publica automáticamente el perfil de marketplace
     * de un doctor en cuanto queda ACTIVE + APPROVED. Se llama desde
     * DoctorService.approveDoctor() para que el doctor aparezca en el
     * marketplace público sin pasos manuales.
     */
    @Transactional
    fun autoPublishForApprovedDoctor(doctor: Doctor): MarketplaceProfileResponse {
        val doctorId = doctor.id
            ?: throw BusinessException("El doctor no tiene un id válido")

        val organization = organizationRepository
            .findBySchemaNameAndStatus(TenantContext.getTenant(), OrganizationStatus.ACTIVE)
            .orElse(null)

        val existingProfile = marketplaceProfileRepository.findByDoctorId(doctorId)

        val resolvedHeadline = resolveDoctorHeadline(doctor)
        val specialtiesJson = buildSpecialtiesJson(doctor)

        val now = LocalDateTime.now()
        val profile = if (existingProfile != null) {
            existingProfile.apply {
                displayName = doctor.fullName
                headline = resolvedHeadline ?: headline
                description = doctor.bio ?: description
                phone = doctor.phone ?: phone
                email = doctor.email ?: email
                city = city ?: organization?.city
                country = country ?: organization?.country
                address = address ?: organization?.address
                profileImageUrl = doctor.profileImageUrl ?: profileImageUrl
                consultationPrice = doctor.consultationPrice ?: consultationPrice
                consultationDurationMinutes =
                    doctor.consultationDurationMinutes ?: consultationDurationMinutes
                availableDays = doctor.availableDays ?: availableDays
                availableStartTime = doctor.availableStartTime ?: availableStartTime
                availableEndTime = doctor.availableEndTime ?: availableEndTime
                specialties = specialtiesJson
                isPublished = true
                status = MarketplaceProfileStatus.PUBLISHED
                publishedAt = publishedAt ?: now
                updatedAt = now
            }
        } else {
            MarketplaceProfile(
                profileType = MarketplaceProfileType.DOCTOR,
                doctorId = doctorId,
                organizationId = organization?.id,
                displayName = doctor.fullName,
                slug = generateUniqueSlug(doctor.fullName),
                headline = resolvedHeadline,
                description = doctor.bio,
                city = organization?.city,
                country = organization?.country,
                address = organization?.address,
                phone = doctor.phone,
                email = doctor.email,
                profileImageUrl = doctor.profileImageUrl,
                consultationPrice = doctor.consultationPrice,
                consultationDurationMinutes = doctor.consultationDurationMinutes,
                availableDays = doctor.availableDays,
                availableStartTime = doctor.availableStartTime,
                availableEndTime = doctor.availableEndTime,
                specialties = specialtiesJson,
                isPublished = true,
                status = MarketplaceProfileStatus.PUBLISHED,
                publishedAt = now
            )
        }

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.syncPublishedProfile(savedProfile)

        return savedProfile.toResponse()
    }

    /**
     * Propaga la foto de perfil de un doctor hacia su perfil de marketplace
     * (y, si ya está publicado, hacia el perfil global visible públicamente).
     * Se llama cada vez que se sube/cambia la foto del doctor, sin importar
     * si el perfil de marketplace ya existía o todavía no fue publicado.
     */
    @Transactional
    fun syncDoctorPhoto(doctorId: UUID, imageUrl: String?) {
        val profile = marketplaceProfileRepository.findByDoctorId(doctorId) ?: return

        profile.profileImageUrl = imageUrl
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        if (savedProfile.isPublished) {
            marketplaceSyncService.syncPublishedProfile(savedProfile)
        }
    }

    /**
     * Oculta del marketplace el perfil asociado a un doctor (si existe).
     * Se usa cuando un doctor se desactiva o se le rechaza la verificación,
     * para que no quede visible públicamente un doctor que ya no debería estarlo.
     */
    @Transactional
    fun hideByDoctorId(doctorId: UUID) {
        val profile = marketplaceProfileRepository.findByDoctorId(doctorId) ?: return

        if (!profile.isPublished) {
            return
        }

        profile.isPublished = false
        profile.status = MarketplaceProfileStatus.HIDDEN
        profile.updatedAt = LocalDateTime.now()

        val savedProfile = marketplaceProfileRepository.save(profile)

        marketplaceSyncService.removePublishedProfile(savedProfile)
    }

    fun autoPublishForActiveOrganization(organization: Organization): MarketplaceProfileResponse {
        val organizationId = organization.id
            ?: throw BusinessException("La organización no tiene un id válido")

        val previousTenant = TenantContext.getTenant()

        try {
            TenantContext.setTenant(organization.schemaName)

            val existingProfile = marketplaceProfileRepository.findByOrganizationIdAndProfileType(organizationId, MarketplaceProfileType.CLINIC)
            val now = LocalDateTime.now()

            val profile = if (existingProfile != null) {
                existingProfile.apply {
                    displayName = organization.name
                    headline = headline ?: organization.type.name
                    city = organization.city ?: city
                    country = organization.country ?: country
                    address = organization.address ?: address
                    phone = organization.phone ?: phone
                    email = organization.email ?: email
                    isPublished = true
                    status = MarketplaceProfileStatus.PUBLISHED
                    publishedAt = publishedAt ?: now
                    updatedAt = now
                }
            } else {
                MarketplaceProfile(
                    profileType = MarketplaceProfileType.CLINIC,
                    doctorId = null,
                    organizationId = organizationId,
                    displayName = organization.name,
                    slug = generateUniqueSlug(organization.name),
                    headline = organization.type.name,
                    description = null,
                    city = organization.city,
                    country = organization.country,
                    address = organization.address,
                    phone = organization.phone,
                    email = organization.email,
                    isPublished = true,
                    status = MarketplaceProfileStatus.PUBLISHED,
                    publishedAt = now
                )
            }

            val savedProfile = marketplaceProfileRepository.save(profile)

            marketplaceSyncService.syncPublishedProfile(savedProfile)

            return savedProfile.toResponse()
        } finally {
            TenantContext.setTenant(previousTenant)
        }
    }

    fun hideByOrganization(organization: Organization) {
        val organizationId = organization.id ?: return
        val previousTenant = TenantContext.getTenant()

        try {
            TenantContext.setTenant(organization.schemaName)

            val profile = marketplaceProfileRepository.findByOrganizationIdAndProfileType(organizationId, MarketplaceProfileType.CLINIC) ?: return

            if (!profile.isPublished) {
                return
            }

            profile.isPublished = false
            profile.status = MarketplaceProfileStatus.HIDDEN
            profile.updatedAt = LocalDateTime.now()

            val savedProfile = marketplaceProfileRepository.save(profile)

            marketplaceSyncService.removePublishedProfile(savedProfile)
        } finally {
            TenantContext.setTenant(previousTenant)
        }
    }

    private fun generateUniqueSlug(fullName: String): String {
        val base = slugify(fullName)
        var candidate = base
        var suffix = 1

        while (isSlugTaken(candidate)) {
            suffix += 1
            candidate = "$base-$suffix"
        }

        return candidate
    }

    /**
     * Comprueba si un slug ya está tomado, revisando tanto la tabla del
     * tenant (marketplace_profiles) como la tabla global
     * (marketplace_profiles_global), que es donde vive el constraint único
     * real y donde antes NO se validaba.
     *
     * Si el slug existe en la tabla global pero el source_profile_id al que
     * apunta ya no existe en el tenant (por ejemplo, porque el doctor/perfil
     * fue borrado sin limpiar su fila global), se considera un registro
     * huérfano: se elimina automáticamente y el slug queda libre para
     * reutilizarse.
     */
    private fun isSlugTaken(candidate: String): Boolean {
        if (marketplaceProfileRepository.existsBySlug(candidate)) {
            return true
        }

        val globalProfile = globalMarketplaceProfileRepository.findBySlug(candidate)
            ?: return false

        val sourceStillExists = globalProfile.sourceProfileId?.let {
            marketplaceProfileRepository.existsById(it)
        } ?: false

        if (sourceStillExists) {
            return true
        }

        // Fila huérfana: el perfil de origen ya no existe en el tenant.
        globalMarketplaceProfileRepository.delete(globalProfile)
        return false
    }
    //revisar que si registran una clinica con un nombre que despues de quitarle
    //tildes y simbolos, quede complemante vacio , ejemplo
    // @@@ o 123 sin ninguna letra, en ese unico caso , el slug
    //de esa clinica terminaria siendo literalmente la palbra doctor
    private fun slugify(input: String): String {
        val withoutAccents = Normalizer
            .normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")

        val slug = withoutAccents
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        return slug.ifBlank { "doctor" }
    }

}
