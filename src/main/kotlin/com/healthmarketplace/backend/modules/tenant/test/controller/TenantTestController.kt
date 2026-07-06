package com.healthmarketplace.backend.modules.tenant.test.controller

import com.healthmarketplace.backend.modules.tenant.test.repository.TestRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tenant-test")
class TenantTestController(
    private val testRepository: TestRepository
) {

    @GetMapping
    fun getData() = testRepository.findAll()
}