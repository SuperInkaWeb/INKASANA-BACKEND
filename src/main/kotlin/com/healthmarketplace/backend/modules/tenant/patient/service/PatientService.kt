package com.healthmarketplace.backend.modules.tenant.patient.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.patient.dto.CreatePatientRequest
import com.healthmarketplace.backend.modules.tenant.patient.dto.PatientResponse
import com.healthmarketplace.backend.modules.tenant.patient.dto.UpdatePatientRequest
import com.healthmarketplace.backend.modules.tenant.patient.entity.Patient
import com.healthmarketplace.backend.modules.tenant.patient.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.patient.model.PatientStatus
import com.healthmarketplace.backend.modules.tenant.patient.repository.PatientRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PatientService(
    private val patientRepository: PatientRepository
) {

    fun findAll(
        status: PatientStatus?,
        search: String?
    ): List<PatientResponse> {

        val patients = when {

            !search.isNullOrBlank() -> {
                val term = search.trim()

                patientRepository
                    .findAllByFullNameContainingIgnoreCaseOrIdentificationContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        term,
                        term,
                        term
                    )
            }

            status != null -> {
                patientRepository
                    .findAllByStatusOrderByCreatedAtDesc(status)
            }

            else -> {
                patientRepository
                    .findAllByOrderByCreatedAtDesc()
            }
        }

        return patients.map { it.toResponse() }
    }

    fun findById(id: UUID): PatientResponse {
        val patient = patientRepository.findById(id)
            .orElseThrow { BusinessException("Paciente no encontrado") }

        return patient.toResponse()
    }

    fun create(request: CreatePatientRequest): PatientResponse {
        val fullName = request.fullName.trim()

        if (fullName.isBlank()) {
            throw BusinessException("El nombre del paciente no puede estar vacío")
        }

        val identification = request.identification?.trim()?.ifBlank { null }

        if (
            identification != null &&
            patientRepository.existsByIdentification(identification)
        ) {
            throw BusinessException("Ya existe un paciente con esa identificación")
        }

        val now = LocalDateTime.now()

        val patient = Patient(
            fullName = fullName,
            identification = identification,
            birthDate = request.birthDate,
            gender = request.gender?.trim()?.ifBlank { null },
            phone = request.phone?.trim()?.ifBlank { null },
            email = request.email?.trim()?.lowercase()?.ifBlank { null },
            address = request.address?.trim()?.ifBlank { null },
            status = PatientStatus.ACTIVE,
            emergencyContactName = request.emergencyContactName?.trim()?.ifBlank { null },
            emergencyContactPhone = request.emergencyContactPhone?.trim()?.ifBlank { null },
            notes = request.notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )

        return patientRepository.save(patient).toResponse()
    }

    fun update(id: UUID, request: UpdatePatientRequest): PatientResponse {
        val patient = patientRepository.findById(id)
            .orElseThrow { BusinessException("Paciente no encontrado") }

        request.fullName?.let {
            val fullName = it.trim()

            if (fullName.isBlank()) {
                throw BusinessException("El nombre del paciente no puede estar vacío")
            }

            patient.fullName = fullName
        }

        request.identification?.let {
            val identification = it.trim().ifBlank { null }

            if (
                identification != null &&
                identification != patient.identification &&
                patientRepository.existsByIdentification(identification)
            ) {
                throw BusinessException("Ya existe un paciente con esa identificación")
            }

            patient.identification = identification
        }

        request.birthDate?.let {
            patient.birthDate = it
        }

        request.gender?.let {
            patient.gender = it.trim().ifBlank { null }
        }

        request.phone?.let {
            patient.phone = it.trim().ifBlank { null }
        }

        request.email?.let {
            patient.email = it.trim().lowercase().ifBlank { null }
        }

        request.address?.let {
            patient.address = it.trim().ifBlank { null }
        }

        request.emergencyContactName?.let {
            patient.emergencyContactName =
                it.trim().ifBlank { null }
        }

        request.emergencyContactPhone?.let {
            patient.emergencyContactPhone =
                it.trim().ifBlank { null }
        }

        request.notes?.let {
            patient.notes =
                it.trim().ifBlank { null }
        }

        patient.updatedAt = LocalDateTime.now()

        return patientRepository.save(patient).toResponse()
    }

    fun deactivate(id: UUID): PatientResponse {

        val patient = patientRepository.findById(id)
            .orElseThrow {
                BusinessException("Paciente no encontrado")
            }

        patient.status = PatientStatus.INACTIVE
        patient.updatedAt = LocalDateTime.now()

        return patientRepository.save(patient).toResponse()
    }

    fun activate(id: UUID): PatientResponse {

        val patient = patientRepository.findById(id)
            .orElseThrow {
                BusinessException("Paciente no encontrado")
            }

        patient.status = PatientStatus.ACTIVE
        patient.updatedAt = LocalDateTime.now()

        return patientRepository.save(patient).toResponse()
    }
}