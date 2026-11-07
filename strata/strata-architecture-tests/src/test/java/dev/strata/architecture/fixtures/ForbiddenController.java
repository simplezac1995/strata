// SPDX-License-Identifier: Apache-2.0
package dev.strata.architecture.fixtures;

import org.springframework.web.bind.annotation.RestController;

/**
 * Deliberately misplaced controller used by the negative-proof test in {@code ArchitectureRulesTest}: a test class so
 * the production import skips it, imported on its own so the controller rule can be proven to fail on it.
 */
@RestController
public class ForbiddenController {}
