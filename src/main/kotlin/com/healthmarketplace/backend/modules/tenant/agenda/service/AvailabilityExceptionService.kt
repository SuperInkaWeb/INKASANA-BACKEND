package com.healthmarketplace.backend.modules.tenant.agenda.service
import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.agenda.dto.AvailabilityExceptionResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.CreateAvailabilityExceptionRequest
import com.healthmarketplace.backend.modules.tenant.agenda.dto.UpdateAvailabilityExceptionRequest
import com.healthmarketplace.backend.modules.tenant.agenda.entity.AvailabilityException
import com.healthmarketplace.backend.modules.tenant.agenda.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.agenda.model.AvailabilityExceptionType
import com.healthmarketplace.backend.modules.tenant.agenda.repository.AvailabilityExceptionRepository
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
class AvailabilityExceptionService(
    private val availabilityExceptionRepository: AvailabilityExceptionRepository,
    private val doctorRepository: DoctorRepository
) {

    @Transactional(readOnly = true)
    fun findAllByDoctor(
        doctorId: UUID,
        from: LocalDate?,
        to: LocalDate?
    ): List<AvailabilityExceptionResponse> {
        requireDoctorExists(doctorId)

        val exceptions = if (from != null && to != null) {
            availabilityExceptionRepository
                .findAllByDoctorIdAndExceptionDateBetweenOrderByExceptionDateAsc(doctorId, from, to)
        } else {
            availabilityExceptionRepository.findAllByDoctorIdOrderByExceptionDateAsc(doctorId)
        }

        return exceptions.map { it.toResponse() }
    }

    @Transactional
    fun create(
        doctorId: UUID,
        request: CreateAvailabilityExceptionRequest
    ): AvailabilityExceptionResponse {
        requireDoctorExists(doctorId)
        requireValidTypeAndTimes(request.type, request.startTime, request.endTime)
        requireNoExceptionOnDate(
            doctorId = doctorId,
            exceptionDate = request.exceptionDate,
            excludingId = null
        )

        val exception = AvailabilityException(
            doctorId = doctorId,
            exceptionDate = request.exceptionDate,
            type = request.type,
            startTime = request.startTime,
            endTime = request.endTime,
            reason = request.reason?.trim()?.ifBlank { null }
        )

        return availabilityExceptionRepository.save(exception).toResponse()
    }

    @Transactional
    fun update(
        doctorId: UUID,
        exceptionId: UUID,
        request: UpdateAvailabilityExceptionRequest
    ): AvailabilityExceptionResponse {
        val exception = findOwnedByDoctor(doctorId, exceptionId)

        val newType = request.type ?: exception.type
        val newStartTime = if (request.type != null) request.startTime else (request.startTime ?: exception.startTime)
        val newEndTime = if (request.type != null) request.endTime else (request.endTime ?: exception.endTime)
        val newExceptionDate = request.exceptionDate ?: exception.exceptionDate

        requireValidTypeAndTimes(newType, newStartTime, newEndTime)
        requireNoExceptionOnDate(
            doctorId = doctorId,
            exceptionDate = newExceptionDate,
            excludingId = exception.id
        )

        exception.exceptionDate = newExceptionDate
        exception.type = newType
        exception.startTime = newStartTime
        exception.endTime = newEndTime
        request.reason?.let { exception.reason = it.trim().ifBlank { null } }
        exception.updatedAt = LocalDateTime.now()

        return availabilityExceptionRepository.save(exception).toResponse()
    }

    @Transactional
    fun delete(doctorId: UUID, exceptionId: UUID) {
        val exception = findOwnedByDoctor(doctorId, exceptionId)
        availabilityExceptionRepository.delete(exception)
    }

    private fun findOwnedByDoctor(doctorId: UUID, exceptionId: UUID): AvailabilityException {
        val exception = availabilityExceptionRepository.findById(exceptionId)
            .orElseThrow { BusinessException("Excepción de disponibilidad no encontrada") }

        if (exception.doctorId != doctorId) {
            throw BusinessException("La excepción no pertenece a este doctor")
        }

        return exception
    }

    private fun requireDoctorExists(doctorId: UUID) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException("Doctor no encontrado")
        }
    }

    // Evita tener dos excepciones para la misma fecha del mismo doctor
    // (ej: no puede estar UNAVAILABLE y a la vez EXTRA el mismo día,
    // ni dos excepciones repetidas para esa fecha).
    private fun requireNoExceptionOnDate(
        doctorId: UUID,
        exceptionDate: LocalDate,
        excludingId: UUID?
    ) {
        val alreadyExists = availabilityExceptionRepository
            .findAllByDoctorIdAndExceptionDate(doctorId, exceptionDate)
            .any { it.id != excludingId }

        if (alreadyExists) {
            throw BusinessException(
                "Ya existe una excepción registrada para esa fecha. " +
                        "Editá o eliminá la existente en vez de crear otra."
            )
        }
    }

    private fun requireValidTypeAndTimes(
        type: AvailabilityExceptionType,
        startTime: LocalTime?,
        endTime: LocalTime?
    ) {
        when (type) {
            AvailabilityExceptionType.UNAVAILABLE -> {
                if (startTime != null || endTime != null) {
                    throw BusinessException(
                        "Una excepción de tipo UNAVAILABLE no debe tener hora de inicio/fin " +
                                "(bloquea el día completo)"
                    )
                }
            }

            AvailabilityExceptionType.EXTRA -> {
                if (startTime == null || endTime == null) {
                    throw BusinessException(
                        "Una excepción de tipo EXTRA requiere hora de inicio y de fin"
                    )
                }

                if (!endTime.isAfter(startTime)) {
                    throw BusinessException("La hora de fin debe ser posterior a la hora de inicio")
                }
            }
        }
    }
}
