package com.healthmarketplace.backend.modules.tenant.agenda.service
import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.tenant.agenda.dto.DaySlotsResponse
import com.healthmarketplace.backend.modules.tenant.agenda.dto.SlotResponse
import com.healthmarketplace.backend.modules.tenant.agenda.model.AvailabilityExceptionType
import com.healthmarketplace.backend.modules.tenant.agenda.repository.AvailabilityExceptionRepository
import com.healthmarketplace.backend.modules.tenant.agenda.repository.DoctorAvailabilityRepository
import com.healthmarketplace.backend.modules.tenant.appointment.model.AppointmentStatus
import com.healthmarketplace.backend.modules.tenant.appointment.repository.AppointmentRepository
import com.healthmarketplace.backend.modules.tenant.doctor.repository.DoctorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class SlotGeneratorService(
    private val doctorRepository: DoctorRepository,
    private val doctorAvailabilityRepository: DoctorAvailabilityRepository,
    private val availabilityExceptionRepository: AvailabilityExceptionRepository,
    private val appointmentRepository: AppointmentRepository
) {

    companion object {
        // Duración por defecto si el doctor no tiene configurada consultationDurationMinutes.
        private const val DEFAULT_SLOT_DURATION_MINUTES = 30L

        // Límite de rango de fechas para evitar consultas demasiado pesadas.
        private const val MAX_RANGE_DAYS = 62L
    }

    // Genera los slots disponibles de un doctor entre "from" y "to" (inclusive).
    // Considera: disponibilidad recurrente semanal, excepciones puntuales
    // (día bloqueado completo o bloque extra) y la duración de consulta del doctor.
    @Transactional(readOnly = true)
    fun generateSlots(doctorId: UUID, from: LocalDate, to: LocalDate): List<DaySlotsResponse> {
        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { BusinessException("Doctor no encontrado") }

        if (to.isBefore(from)) {
            throw BusinessException("La fecha 'to' no puede ser anterior a 'from'")
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw BusinessException("El rango máximo permitido es de $MAX_RANGE_DAYS días")
        }

        val durationMinutes = doctor.consultationDurationMinutes
            ?.takeIf { it > 0 }
            ?.toLong()
            ?: DEFAULT_SLOT_DURATION_MINUTES

        // Excepciones puntuales del rango, agrupadas por fecha para acceso rápido.
        val exceptionsByDate = availabilityExceptionRepository
            .findAllByDoctorIdAndExceptionDateBetweenOrderByExceptionDateAsc(doctorId, from, to)
            .groupBy { it.exceptionDate }

        // Disponibilidad recurrente activa, agrupada por día de la semana.
        val recurringByDayOfWeek = doctorAvailabilityRepository
            .findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
            .filter { it.active }
            .groupBy { it.dayOfWeek }

        val now = LocalDateTime.now()
        val bookedSlots = appointmentRepository
            .findAllByDoctorIdAndDateBetweenAndStatusNot(doctorId, from, to, AppointmentStatus.CANCELLED)
            .map { it.date to it.time }
            .toSet()
        val result = mutableListOf<DaySlotsResponse>()

        var date = from
        while (!date.isAfter(to)) {
            val dayExceptions = exceptionsByDate[date].orEmpty()

            // Si el doctor marcó el día completo como no disponible, no hay slots.
            val dayBlocked = dayExceptions.any { it.type == AvailabilityExceptionType.UNAVAILABLE }

            val slots = if (dayBlocked) {
                emptyList()
            } else {
                val recurringBlocks = recurringByDayOfWeek[date.dayOfWeek]
                    .orEmpty()
                    .map { it.startTime to it.endTime }

                val extraBlocks = dayExceptions
                    .filter { it.type == AvailabilityExceptionType.EXTRA }
                    .mapNotNull { exception ->
                        val start = exception.startTime
                        val end = exception.endTime
                        if (start != null && end != null) start to end else null
                    }

                (recurringBlocks + extraBlocks)
                    .flatMap { (blockStart, blockEnd) ->
                        generateSlotsForBlock(blockStart, blockEnd, durationMinutes)
                    }
                    .distinct()
                    .sortedBy { it.first }
                    .map { (slotStart, slotEnd) ->
                        SlotResponse(
                            startTime = slotStart,
                            endTime = slotEnd,
                            available = LocalDateTime.of(date, slotStart).isAfter(now) && (date to slotStart) !in bookedSlots
                        )
                    }
            }

            result.add(DaySlotsResponse(date = date, slots = slots))
            date = date.plusDays(1)
        }

        return result
    }

    // Parte un bloque [blockStart, blockEnd) en slots consecutivos de "durationMinutes".
    // Descarta el último tramo si no alcanza a completar la duración exacta.
    private fun generateSlotsForBlock(
        blockStart: LocalTime,
        blockEnd: LocalTime,
        durationMinutes: Long
    ): List<Pair<LocalTime, LocalTime>> {
        if (!blockEnd.isAfter(blockStart)) return emptyList()

        val slots = mutableListOf<Pair<LocalTime, LocalTime>>()
        var cursor = blockStart

        while (true) {
            val slotEnd = cursor.plusMinutes(durationMinutes)

            // slotEnd.isAfter(cursor) es falso si LocalTime dio la vuelta a la
            // medianoche (ej: 23:50 + 30min = 00:20), lo que corta el loop a salvo.
            if (!slotEnd.isAfter(cursor) || slotEnd.isAfter(blockEnd)) break

            slots.add(cursor to slotEnd)
            cursor = slotEnd
        }

        return slots
    }
}
