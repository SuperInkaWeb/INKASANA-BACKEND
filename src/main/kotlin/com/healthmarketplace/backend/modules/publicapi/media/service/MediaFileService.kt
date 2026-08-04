package com.healthmarketplace.backend.modules.publicapi.media.service

import com.healthmarketplace.backend.common.exception.BusinessException
import com.healthmarketplace.backend.modules.publicapi.media.entity.MediaFile
import com.healthmarketplace.backend.modules.publicapi.media.repository.MediaFileRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class MediaFileService(
    private val mediaFileRepository: MediaFileRepository
) {

    companion object {
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    }

    fun storeImage(file: MultipartFile): MediaFile {
        if (file.isEmpty) {
            throw BusinessException("El archivo de imagen está vacío")
        }

        if (file.size > MAX_FILE_SIZE_BYTES) {
            throw BusinessException("La imagen no puede superar los 5 MB")
        }

        val contentType = file.contentType?.lowercase()

        if (contentType == null || contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(
                "Formato de imagen no soportado. Usa JPG, PNG o WEBP"
            )
        }

        val mediaFile = MediaFile(
            contentType = contentType,
            fileSize = file.size.toInt(),
            fileData = file.bytes
        )

        return mediaFileRepository.save(mediaFile)
    }

    fun findById(id: UUID): MediaFile {
        return mediaFileRepository.findById(id)
            .orElseThrow { BusinessException("Archivo no encontrado") }
    }
}
