// SPDX-License-Identifier: Apache-2.0
package dev.strata.architecture.fixtures.cycle.a;

import dev.strata.architecture.fixtures.cycle.b.ModuleB;

/**
 * Deliberately half of a module cycle: {@code a.ModuleA} references {@code b.ModuleB} and {@code b.ModuleB} references
 * this class. Only used by the negative-proof test of the Spring Modulith cycle gate.
 */
public final class ModuleA {

    private ModuleA() {}

    static final Class<?> PARTNER = ModuleB.class;
}
