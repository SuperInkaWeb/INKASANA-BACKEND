package com.healthmarketplace.backend.modules.tenant.agenda.dto

import java.time.LocalDate
import java.time.LocalTime

// Un turno/slot puntual dentro de un día.
data class SlotResponse(
    val startTime: LocalTime,
    val endTime: LocalTime,
    // Por ahora "available" solo descarta slots que ya pasaron (hoy).
    // Cuando exista el módulo de Citas/Appointments, aquí se debe cruzar
    // también contra las citas ya reservadas para marcar el slot como ocupado.
    val available: Boolean
)

// Slots generados para un día específico.
data class DaySlotsResponse(
    val date: LocalDate,
    val slots: List<SlotResponse>
)
