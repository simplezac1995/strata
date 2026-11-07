// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** Logical module, e.g. {@code demo:product}. */
    String module();

    /** Action verb: create | update | delete. */
    String action();
}
