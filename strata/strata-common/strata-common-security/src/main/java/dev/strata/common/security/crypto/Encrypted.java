// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark entity fields that should be encrypted at rest.
 *
 * <p>Fields annotated with {@code @Encrypted} will be automatically encrypted before
 * persisting to the database and decrypted when read from the database.
 *
 * <p>Usage:
 * <pre>{@code
 * @Entity
 * public class User {
 *     @Encrypted
 *     private String email;
 *
 *     @Encrypted
 *     private String phoneNumber;
 * }
 * }</pre>
 *
 * <p>Requires {@link EncryptedFieldListener} to be registered as a JPA entity listener.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {}
