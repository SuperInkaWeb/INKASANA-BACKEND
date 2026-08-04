package com.healthmarketplace.backend.modules.tenant.agenda.service
import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.agenda.dto.CreateDoctorAvailabilityRequest
import com.healthmarketplace.backend.modules.tenant.agenda.dto.DoctorAvailabilityResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.UpdateDoctorAvailabilityRequest
import com.healthmarketplace.backend.modules.tenant.agenda.entity.DoctorAvailability
import com.healthmarketplace.backend.modules.tenant.agenda.mapper.toResponse
import com.healthmarketplace.backend.modules.tenant.agenda.repository.DoctorAvailabilityRepository
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.UUID

@Service
class DoctorAvailabilityService(
    private val doctorAvailabilityRepository: DoctorAvailabilityRepository,
    private val doctorRepository: DoctorRepository
) {

    @Transactional(readOnly = true)
    fun findAllByDoctor(doctorId: UUID): List<DoctorAvailabilityResponse> {
        requireDoctorExists(doctorId)

        return doctorAvailabilityRepository
            .findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
            .map { it.toResponse() }
    }

    @Transactional
    fun create(
        doctorId: UUID,
        request: CreateDoctorAvailabilityRequest
    ): DoctorAvailabilityResponse {
        requireDoctorExists(doctorId)
        requireValidTimeRange(request.startTime, request.endTime)

        val existingForDay = doctorAvailabilityRepository
            .findAllByDoctorIdAndDayOfWeekOrderByStartTimeAsc(doctorId, request.dayOfWeek)

        requireNoOverlap(
            existing = existingForDay,
            startTime = request.startTime,
            endTime = request.endTime,
            excludingId = null
        )

        val availability = DoctorAvailability(
            doctorId = doctorId,
            dayOfWeek = request.dayOfWeek,
            startTime = request.startTime,
            endTime = request.endTime,
            active = request.active
        )

        return doctorAvailabilityRepository.save(availability).toResponse()
    }

    @Transactional
    fun update(
        doctorId: UUID,
        availabilityId: UUID,
        request: UpdateDoctorAvailabilityRequest
    ): DoctorAvailabilityResponse {
        val availability = findOwnedByDoctor(doctorId, availabilityId)

        val newDayOfWeek = request.dayOfWeek ?: availability.dayOfWeek
        val newStartTime = request.startTime ?: availability.startTime
        val newEndTime = request.endTime ?: availability.endTime

        requireValidTimeRange(newStartTime, newEndTime)

        val existingForDay = doctorAvailabilityRepository
            .findAllByDoctorIdAndDayOfWeekOrderByStartTimeAsc(doctorId, newDayOfWeek)

        requireNoOverlap(
            existing = existingForDay,
            startTime = newStartTime,
            endTime = newEndTime,
            excludingId = availability.id
        )

        availability.dayOfWeek = newDayOfWeek
        availability.startTime = newStartTime
        availability.endTime = newEndTime
        request.active?.let { availability.active = it }
        availability.updatedAt = LocalDateTime.now()

        return doctorAvailabilityRepository.save(availability).toResponse()
    }

    @Transactional
    fun delete(doctorId: UUID, availabilityId: UUID) {
        val availability = findOwnedByDoctor(doctorId, availabilityId)
        doctorAvailabilityRepository.delete(availability)
    }

    private fun findOwnedByDoctor(doctorId: UUID, availabilityId: UUID): DoctorAvailability {
        val availability = doctorAvailabilityRepository.findById(availabilityId)
            .orElseThrow { BusinessException("Horario no encontrado") }

        if (availability.doctorId != doctorId) {
            throw BusinessException("El horario no pertenece a este doctor")
        }

        return availability
    }

    private fun requireDoctorExists(doctorId: UUID) {
        if (!doctorRepository.existsById(doctorId)) {
            throw BusinessException("Doctor no encontrado")
        }
    }

    private fun requireValidTimeRange(startTime: java.time.LocalTime, endTime: java.time.LocalTime) {
        if (!endTime.isAfter(startTime)) {
            throw BusinessException("La hora de fin debe ser posterior a la hora de inicio")
        }
    }

    // Evita crear dos bloques de horario que se pisen el mismo día para el
    // mismo doctor (ej: 08:00-12:00 y 10:00-14:00 no pueden coexistir).
    private fun requireNoOverlap(
        existing: List<DoctorAvailability>,
        startTime: java.time.LocalTime,
        endTime: java.time.LocalTime,
        excludingId: UUID?
    ) {
        val overlaps = existing
            .filter { it.id != excludingId }
            .any { block ->
                startTime.isBefore(block.endTime) && endTime.isAfter(block.startTime)
            }

        if (overlaps) {
            throw BusinessException(
                "Ya existe un bloque de horario que se cruza con ese rango ese día"
            )
        }
    }
}
