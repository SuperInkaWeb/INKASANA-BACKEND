package com.healthmarketplace.backend.modules.tenant.doctor.service

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
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserRole
import com.healthmarketplace.backend.modules.tenant.user.model.TenantUserStatus
import com.healthmarketplace.backend.modules.tenant.user.repository.TenantUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val tenantUserRepository: TenantUserRepository,
    private val doctorSpecialtyRepository: DoctorSpecialtyRepository,
    private val globalSpecialtyRepository: GlobalSpecialtyRepository
) {

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

        return doctors.map { doctor ->
            doctor.toResponse(
                specialties = getDoctorSpecialties(doctor.id!!)
            )
        }
    }

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

        doctor.updatedAt = LocalDateTime.now()

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
    fun deactivate(id: UUID): DoctorResponse {
        val doctor = doctorRepository.findById(id)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        doctor.status = DoctorStatus.INACTIVE
        doctor.updatedAt = LocalDateTime.now()

        val savedDoctor = doctorRepository.save(doctor)

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