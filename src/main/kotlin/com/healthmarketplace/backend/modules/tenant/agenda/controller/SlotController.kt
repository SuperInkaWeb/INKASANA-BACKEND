package com.healthmarketplace.backend.modules.tenant.agenda.controller
import com.healthmarketplace.backend.modules.tenant.agenda.dto.DaySlotsResponse
import com.healthmarketplace.backend.modules.tenant.agenda.service.SlotGeneratorService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

// GET /api/tenant/doctors/{doctorId}/slots?from=2026-07-24&to=2026-07-28
// "to" es opcional: si no se envía, se generan los slots solo para "from".
@RestController
@RequestMapping("/api/tenant/doctors/{doctorId}/slots")
class SlotController(
    private val slotGeneratorService: SlotGeneratorService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','DOCTOR','THERAPIST','RECEPTIONIST')")
    fun findSlots(
        @PathVariable doctorId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): List<DaySlotsResponse> {
        return slotGeneratorService.generateSlots(doctorId, from, to ?: from)
    }
}
