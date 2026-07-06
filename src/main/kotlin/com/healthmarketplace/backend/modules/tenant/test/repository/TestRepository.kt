package com.healthmarketplace.backend.modules.tenant.test.repository

import com.healthmarketplace.backend.modules.tenant.test.entity.TestEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TestRepository : JpaRepository<TestEntity, UUID>