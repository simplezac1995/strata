// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for sensitive data desensitization in API responses.
 *
 * <p>Apply this annotation to fields containing sensitive information to automatically
 * mask them in JSON responses. Supports multiple desensitization strategies.
 *
 * <p>Usage:
 * <pre>{@code
 * public class UserDto {
 *     @Desensitize(type = DesensitizeType.PHONE)
 *     private String phone;
 *
 *     @Desensitize(type = DesensitizeType.EMAIL)
 *     private String email;
 *
 *     @Desensitize(type = DesensitizeType.ID_CARD)
 *     private String idCard;
 *
 *     @Desensitize(type = DesensitizeType.CUSTOM, keepLeft = 2, keepRight = 2)
 *     private String bankCard;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    /**
     * Desensitization strategy type.
     */
    DesensitizeType type() default DesensitizeType.CUSTOM;

    /**
     * Number of characters to keep at the start (for CUSTOM type).
     */
    int keepLeft() default 0;

    /**
     * Number of characters to keep at the end (for CUSTOM type).
     */
    int keepRight() default 0;

    /**
     * Mask character to use.
     */
    char maskChar() default '*';
}
