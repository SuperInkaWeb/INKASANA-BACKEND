package com.healthmarketplace.backend.modules.publicapi.media.repository

import com.healthmarketplace.backend.modules.publicapi.media.entity.MediaFile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MediaFileRepository : JpaRepository<MediaFile, UUID>
