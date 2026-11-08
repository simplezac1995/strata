// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.gradle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal Gradle application: the starter auto-configures RBAC, security, Flyway and the admin SPA. */
@SpringBootApplication
public class GradleMinimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradleMinimalApplication.class, args);
    }

    @RestController
    static class HelloController {

        @GetMapping("/api/hello")
        String hello() {
            return "hello from a Gradle Strata application";
        }
    }
}
