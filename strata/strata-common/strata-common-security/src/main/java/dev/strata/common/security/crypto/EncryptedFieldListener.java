// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.lang.reflect.Field;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener that automatically encrypts and decrypts fields annotated with {@link Encrypted}.
 *
 * <p>Usage on entity:
 * <pre>{@code
 * @Entity
 * @EntityListeners(EncryptedFieldListener.class)
 * public class User {
 *     @Encrypted
 *     private String email;
 * }
 * }</pre>
 *
 * <p>Note: This listener requires Spring dependency injection via {@code @Configurable}
 * or manual wiring. For Spring Data JPA, register via {@code @EntityListeners} and
 * ensure {@link FieldEncryptor} is available as a Spring bean.
 */
@Component
public class EncryptedFieldListener {

    private static FieldEncryptor fieldEncryptor;

    /**
     * Set the FieldEncryptor instance. Called by Spring configuration during startup.
     */
    public static void setFieldEncryptor(FieldEncryptor encryptor) {
        fieldEncryptor = encryptor;
    }

    @PrePersist
    @PreUpdate
    public void encrypt(Object entity) {
        if (fieldEncryptor == null) {
            return;
        }
        processFields(entity, true);
    }

    @PostLoad
    public void decrypt(Object entity) {
        if (fieldEncryptor == null) {
            return;
        }
        processFields(entity, false);
    }

    private void processFields(Object entity, boolean encrypt) {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Encrypted.class) && field.getType() == String.class) {
                    try {
                        field.setAccessible(true);
                        String value = (String) field.get(entity);
                        if (value != null && !value.isEmpty()) {
                            if (encrypt) {
                                if (!fieldEncryptor.isEncrypted(value)) {
                                    field.set(entity, fieldEncryptor.encrypt(value));
                                }
                            } else {
                                if (fieldEncryptor.isEncrypted(value)) {
                                    field.set(entity, fieldEncryptor.decrypt(value));
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to process encrypted field: " + field.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
