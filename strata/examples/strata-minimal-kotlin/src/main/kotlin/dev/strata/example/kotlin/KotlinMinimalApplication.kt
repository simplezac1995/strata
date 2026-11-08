// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Minimal Kotlin application: the starter auto-configures RBAC, security, Flyway and the admin SPA. */
@SpringBootApplication
class KotlinMinimalApplication

fun main(args: Array<String>) {
    runApplication<KotlinMinimalApplication>(*args)
}

@RestController
class HelloController {
    @GetMapping("/api/hello")
    fun hello(): String = "hello from a Kotlin Strata application"
}
