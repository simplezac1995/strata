// SPDX-License-Identifier: Apache-2.0
package dev.strata.example.minimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The whole consumer application: no component scan, no entity scan, no repository scan. The starter's
 * auto-configuration wires the framework; the optional AI modules wire themselves when their jar is on the classpath.
 *
 * <p>Run it with {@code mvn spring-boot:run} after starting PostgreSQL (see {@code README.md}). Swagger UI is at
 * {@code /swagger-ui.html}, the admin API is under {@code /api/v1}.
 */
@SpringBootApplication
public class MinimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimalApplication.class, args);
    }
}
