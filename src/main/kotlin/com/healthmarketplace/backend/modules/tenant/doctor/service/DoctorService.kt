package com.healthmarketplace.backend.modules.tenant.doctor.service
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.core.specialty.repository.GlobalSpecialtyRepository
import com.healthmarketplace.backend.modules.tenant.doctor.dto.CreateDoctorRequest
import com.healthmarketplace.backend.modules.tenant.doctor.dto.DoctorResponse
import com.healthmarketplace.backend.modules.tenant.doctor.dto.DoctorSpecialtyResponse
import com.healthmarketplace.backend.modules.tenant.doctor.dto.UpdateDoctorRequest
import com.healthmarketplace.backend.modules.tenant.doctor.entity.Doctor
import com.healthmarketplace.backend.modules.tenant.doctor.entity.DoctorSpecialty
import com.healthmarketplace.backend.modules.tenant.doctor.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorStatus
import com.healthmarketplace.backend.modules.tenant.doctor.model.DoctorVerificationStatus
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorSpecialtyRepository
import com.healthmarketplace.backend.modules.tenant.marketplace.service.MarketplaceProfileService
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import com.healthmarketplace.backend.modules.publicapi.media.service.MediaFileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDateTime
import java.util.UUID
@Service
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val tenantUserRepository: TenantUserRepository,
    private val doctorSpecialtyRepository: DoctorSpecialtyRepository,
    private val globalSpecialtyRepository: GlobalSpecialtyRepository,
    private val marketplaceProfileService: MarketplaceProfileService,
    private val mediaFileService: MediaFileService
) {

    @Transactional(readOnly = true)
    fun findAll(
        status: DoctorStatus?,
        search: String?
    ): List<DoctorResponse> {

        val doctors = when {

            !search.isNullOrBlank() -> {
                val term = search.trim()

                doctorRepository
                    .findAllByFullNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        term,
                        term,
                        term
                    )
            }

            status != null -> {
                doctorRepository.findAllByStatusOrderByCreatedAtDesc(status)
            }

            else -> {
                doctorRepository.findAllByOrderByCreatedAtDesc()
            }
        }

        if (doctors.isEmpty()) {
            return emptyList()
        }

        // Antes: por cada doctor se hacían 2 consultas extra (relaciones +
        // especialidades), es decir 1 + 2*N consultas para listar N
        // doctores. Con varias decenas de doctores eso abre y cierra
        // muchísimas conexiones contra el pool (cada una re-fijando el
        // schema del tenant), lo que producía la demora ("carga pero
        // demorando mucho") e incluso agotaba el pool de conexiones bajo
        // carga. Ahora se traen TODAS las relaciones y especialidades en
        // una sola consulta cada una, y se agrupan en memoria.
        val doctorIds = doctors.mapNotNull { it.id }

        val allRelations = doctorSpecialtyRepository.findAllByDoctorIdIn(doctorIds)
        val relationsByDoctorId = allRelations.groupBy { it.doctorId }

        val allSpecialtyIds = allRelations.map { it.specialtyId }.distinct()
        val specialtiesById = if (allSpecialtyIds.isEmpty()) {
            emptyMap()
        } else {
            globalSpecialtyRepository.findAllById(allSpecialtyIds).associateBy { it.id }
        }

        return doctors.map { doctor ->
            val specialties = relationsByDoctorId[doctor.id]
                .orEmpty()
                .mapNotNull { relation -> specialtiesById[relation.specialtyId] }
                .map {
                    DoctorSpecialtyResponse(
                        id = it.id!!,
                        name = it.name,
                        description = it.description
                    )
                }

            doctor.toResponse(specialties = specialties)
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        return doctor.toResponse(
            specialties = getDoctorSpecialties(doctor.id!!)
        )
    }

    @Transactional
    fun create(request: CreateDoctorRequest): DoctorResponse {
        val fullName = request.fullName.trim()

        if (fullName.isBlank()) {
            throw BusinessException("El nombre del doctor no puede estar vacío")
        }

        val tenantUser = tenantUserRepository.findById(request.tenantUserId)
            .orElseThrow { BusinessException("Usuario tenant no encontrado") }

        if (tenantUser.role != TenantUserRole.DOCTOR) {
            throw BusinessException("El usuario seleccionado no tiene rol DOCTOR")
        }

        if (tenantUser.status != TenantUserStatus.ACTIVE) {
            throw BusinessException("El usuario doctor no está activo")
        }

        if (doctorRepository.findByTenantUserId(request.tenantUserId).isPresent) {
            throw BusinessException("Este usuario doctor ya tiene un perfil médico registrado")
        }

        val licenseNumber = request.licenseNumber?.trim()?.ifBlank { null }

        if (licenseNumber == null) {
            throw BusinessException("La licencia profesional es obligatoria")
        }

        if (doctorRepository.existsByLicenseNumber(licenseNumber)) {
            throw BusinessException("Ya existe un doctor con ese número de licencia")
        }

        val now = LocalDateTime.now()

        val doctor = Doctor(
            tenantUserId = tenantUser.id,
            fullName = fullName,
            specialty = request.specialty?.trim()?.ifBlank { null },
            licenseNumber = licenseNumber,
            email = request.email?.trim()?.lowercase()?.ifBlank { null } ?: tenantUser.email,
            phone = request.phone?.trim()?.ifBlank { null } ?: tenantUser.phone,
            status = DoctorStatus.ACTIVE,
            bio = request.bio?.trim()?.ifBlank { null },
            consultationPrice = request.consultationPrice,
            consultationDurationMinutes = request.consultationDurationMinutes,
            profileImageUrl = request.profileImageUrl?.trim()?.ifBlank { null },
            availableDays = request.availableDays
                .takeIf { it.isNotEmpty() }
                ?.joinToString(","),
            availableStartTime = request.availableStartTime?.trim()?.ifBlank { null },
            availableEndTime = request.availableEndTime?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )

        val savedDoctor = doctorRepository.save(doctor)

        syncDoctorSpecialties(
            doctorId = savedDoctor.id!!,
            specialtyIds = request.specialtyIds
        )

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    @Transactional
    fun update(id: UUID, request: UpdateDoctorRequest): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        request.tenantUserId?.let { tenantUserId ->
            if (tenantUserId == doctor.tenantUserId) {
                // No cambió el usuario asignado: no hace falta revalidar
                // su estado, para no bloquear ediciones normales (como
                // subir la foto) por algo que ni se está modificando.
                return@let
            }

            val tenantUser = tenantUserRepository.findById(tenantUserId)
                .orElseThrow { BusinessException("Usuario tenant no encontrado") }

            if (tenantUser.role != TenantUserRole.DOCTOR) {
                throw BusinessException("El usuario seleccionado no tiene rol DOCTOR")
            }

            if (tenantUser.status != TenantUserStatus.ACTIVE) {
                throw BusinessException("El usuario doctor no está activo")
            }

            val existingDoctor = doctorRepository.findByTenantUserId(tenantUserId)

            if (
                existingDoctor.isPresent &&
                existingDoctor.get().id != doctor.id
            ) {
                throw BusinessException("Este usuario doctor ya tiene un perfil médico registrado")
            }

            doctor.tenantUserId = tenantUserId
        }

        request.fullName?.let {
            val fullName = it.trim()

            if (fullName.isBlank()) {
                throw BusinessException("El nombre del doctor no puede estar vacío")
            }

            doctor.fullName = fullName
        }

        request.specialty?.let {
            doctor.specialty = it.trim().ifBlank { null }
        }

        request.licenseNumber?.let {
            val licenseNumber = it.trim().ifBlank { null }

            if (licenseNumber == null) {
                throw BusinessException("La licencia profesional es obligatoria")
            }

            if (
                licenseNumber != doctor.licenseNumber &&
                doctorRepository.existsByLicenseNumberAndIdNot(
                    licenseNumber,
                    id
                )
            ) {
                throw BusinessException("Ya existe un doctor con ese número de licencia")
            }

            doctor.licenseNumber = licenseNumber
        }

        request.email?.let {
            doctor.email = it.trim().lowercase().ifBlank { null }
        }

        request.phone?.let {
            doctor.phone = it.trim().ifBlank { null }
        }

        request.bio?.let {
            doctor.bio = it.trim().ifBlank { null }
        }

        request.consultationPrice?.let {
            doctor.consultationPrice = it
        }

        request.consultationDurationMinutes?.let {
            doctor.consultationDurationMinutes = it
        }

        request.profileImageUrl?.let {
            doctor.profileImageUrl = it.trim().ifBlank { null }
        }

        request.availableDays?.let {
            doctor.availableDays = it.takeIf { days -> days.isNotEmpty() }?.joinToString(",")
        }

        request.availableStartTime?.let {
            doctor.availableStartTime = it.trim().ifBlank { null }
        }

        request.availableEndTime?.let {
            doctor.availableEndTime = it.trim().ifBlank { null }
        }

        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        syncDoctorSpecialties(
            doctorId = savedDoctor.id!!,
            specialtyIds = request.specialtyIds
        )

        if (
            savedDoctor.verificationStatus == DoctorVerificationStatus.APPROVED &&
            savedDoctor.status == DoctorStatus.ACTIVE
        ) {
            marketplaceProfileService.autoPublishForApprovedDoctor(savedDoctor)
        }

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    /**
     * Sube una imagen (arrastrada/soltada desde el formulario) y la asocia
     * como foto de perfil del doctor. La imagen se guarda en un almacenamiento
     * público independiente del tenant, y la URL resultante se guarda en
     * `profileImageUrl`. También se sincroniza hacia el perfil de marketplace
     * (si ya existe), para que la foto aparezca en el card público y en el
     * detalle del doctor sin pasos manuales adicionales.
     */
    @Transactional
    fun updateProfilePhoto(id: UUID, file: MultipartFile): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        val mediaFile = mediaFileService.storeImage(file)

        val publicUrl = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/public/media/${mediaFile.id}")
            .toUriString()

        doctor.profileImageUrl = publicUrl
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        marketplaceProfileService.syncDoctorPhoto(
            doctorId = savedDoctor.id!!,
            imageUrl = publicUrl
        )

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    @Transactional
    fun deactivate(id: UUID): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        doctor.status = DoctorStatus.INACTIVE
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        marketplaceProfileService.hideByDoctorId(savedDoctor.id!!)

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    @Transactional
    fun activate(id: UUID): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        doctor.status = DoctorStatus.ACTIVE
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        if (savedDoctor.verificationStatus == DoctorVerificationStatus.APPROVED) {
            marketplaceProfileService.autoPublishForApprovedDoctor(savedDoctor)
        }

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    @Transactional
    fun approveDoctor(
        id: UUID,
        verifiedBy: UUID
    ): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        if (doctor.verificationStatus == DoctorVerificationStatus.APPROVED) {
            throw BusinessException("El doctor ya se encuentra aprobado")
        }

        doctor.verificationStatus = DoctorVerificationStatus.APPROVED
        doctor.verifiedAt = LocalDateTime.now()
        doctor.verifiedBy = verifiedBy
        doctor.rejectionReason = null
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        if (savedDoctor.status == DoctorStatus.ACTIVE) {
            marketplaceProfileService.autoPublishForApprovedDoctor(savedDoctor)
        }

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    @Transactional
    fun rejectDoctor(
        id: UUID,
        verifiedBy: UUID,
        reason: String
    ): DoctorResponse {
        val rejectionReason = reason.trim()

        if (rejectionReason.isBlank()) {
            throw BusinessException("El motivo de rechazo es obligatorio")
        }

        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        doctor.verificationStatus = DoctorVerificationStatus.REJECTED
        doctor.verifiedAt = LocalDateTime.now()
        doctor.verifiedBy = verifiedBy
        doctor.rejectionReason = rejectionReason
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

        marketplaceProfileService.hideByDoctorId(savedDoctor.id!!)

        return savedDoctor.toResponse(
            specialties = getDoctorSpecialties(savedDoctor.id!!)
        )
    }

    private fun getDoctorSpecialties(
        doctorId: UUID
    ): List<DoctorSpecialtyResponse> {
        val relations = doctorSpecialtyRepository.findAllByDoctorId(doctorId)

        if (relations.isEmpty()) {
            return emptyList()
        }

        val specialtyIds = relations.map { it.specialtyId }

        val specialties = globalSpecialtyRepository.findAllById(specialtyIds)

        return specialties.map {
            DoctorSpecialtyResponse(
                id = it.id!!,
                name = it.name,
                description = it.description
            )
        }
    }

    private fun syncDoctorSpecialties(
        doctorId: UUID,
        specialtyIds: List<UUID>
    ) {
        val uniqueSpecialtyIds = specialtyIds.distinct()

        doctorSpecialtyRepository.deleteAllByDoctorId(doctorId)
        doctorSpecialtyRepository.flush()

        if (uniqueSpecialtyIds.isEmpty()) {
            return
        }

        val specialties = globalSpecialtyRepository.findAllById(uniqueSpecialtyIds)

        if (specialties.size != uniqueSpecialtyIds.size) {
            throw BusinessException("Una o más especialidades no existen")
        }

        val relations = uniqueSpecialtyIds.map { specialtyId ->
            DoctorSpecialty(
                doctorId = doctorId,
                specialtyId = specialtyId
            )
        }

        doctorSpecialtyRepository.saveAll(relations)
    }
}