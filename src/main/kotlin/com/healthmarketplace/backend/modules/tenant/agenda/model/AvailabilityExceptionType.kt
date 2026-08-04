package com.healthmarketplace.backend.modules.tenant.agenda.model

enum class AvailabilityExceptionType {
    // El doctor no atiende en absoluto ese día (vacaciones, feriado, licencia, etc.)
    UNAVAILABLE,

    // Bloque adicional de atención ese día, fuera de su horario recurrente habitual.
    EXTRA
}
