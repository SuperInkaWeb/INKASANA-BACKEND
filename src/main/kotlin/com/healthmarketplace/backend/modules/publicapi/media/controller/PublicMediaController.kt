package com.healthmarketplace.backend.modules.publicapi.media.controller

import com.healthmarketplace.backend.modules.publicapi.media.service.MediaFileService
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/public/media")
class PublicMediaController(
    private val mediaFileService: MediaFileService
) {

    @GetMapping("/{id}")
    fun getMedia(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val mediaFile = mediaFileService.findById(id)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(mediaFile.contentType))
            .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
            .body(mediaFile.fileData)
    }
}
