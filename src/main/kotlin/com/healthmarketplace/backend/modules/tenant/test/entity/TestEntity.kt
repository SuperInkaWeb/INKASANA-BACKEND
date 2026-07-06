package com.healthmarketplace.backend.modules.tenant.test.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "test_table")
class TestEntity(

    @Id
    var id: UUID? = null,

    var name: String = ""
)