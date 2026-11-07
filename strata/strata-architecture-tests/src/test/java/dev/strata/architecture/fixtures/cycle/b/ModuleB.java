// SPDX-License-Identifier: Apache-2.0
package dev.strata.architecture.fixtures.cycle.b;

import dev.strata.architecture.fixtures.cycle.a.ModuleA;

/**
 * Deliberately the other half of a module cycle: {@code b.ModuleB} references {@code a.ModuleA} and {@code a.ModuleA}
 * references this class. Only used by the negative-proof test of the Spring Modulith cycle gate.
 */
public final class ModuleB {

    private ModuleB() {}

    static final Class<?> PARTNER = ModuleA.class;
}
