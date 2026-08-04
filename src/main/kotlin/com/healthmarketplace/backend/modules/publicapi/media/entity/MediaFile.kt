package com.healthmarketplace.backend.modules.publicapi.media.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Archivo binario (imagen) almacenado en el schema public, fuera de
 * cualquier tenant. Se usa para servir contenido públicamente (por ejemplo,
 * la foto de perfil de un doctor) sin necesitar resolver el schema del
 * tenant, ya que las peticiones GET públicas no llevan JWT.
 */
@Entity
@Table(name = "media_files", schema = "public")
class MediaFile(

    @Id
    @GeneratedValue
    var id: UUID? = null,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Int,

    @Column(name = "file_data", nullable = false, columnDefinition = "bytea")
    var fileData: ByteArray,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
