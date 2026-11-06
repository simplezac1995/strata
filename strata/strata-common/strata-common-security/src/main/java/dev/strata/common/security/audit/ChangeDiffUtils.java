// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for comparing two objects and generating field-level change diffs.
 *
 * <p>Used by audit logging to record what exactly changed in an update operation.
 * Supports:
 * <ul>
 *   <li>Field-by-field comparison using reflection</li>
 *   <li>Ignoring specific fields (e.g., id, createdAt, updatedAt)</li>
 *   <li>Nested object comparison (shallow)</li>
 *   <li>JSON serialization of diff results</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * Map<String, FieldChange> diff = ChangeDiffUtils.diff(oldUser, newUser);
 * String json = ChangeDiffUtils.toJson(diff);
 * }</pre>
 */
public final class ChangeDiffUtils {

    private static final Logger log = LoggerFactory.getLogger(ChangeDiffUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Fields to ignore by default (audit fields, IDs, timestamps). */
    private static final List<String> DEFAULT_IGNORED_FIELDS = List.of(
            "id", "createdAt", "updatedAt", "createdBy", "updatedBy", "version", "serialVersionUID", "logger", "log");

    private ChangeDiffUtils() {}

    /**
     * Compare two objects and return field-level changes.
     *
     * @param oldValue the old object state
     * @param newValue the new object state
     * @return map of field name to FieldChange (only changed fields)
     */
    public static Map<String, FieldChange> diff(Object oldValue, Object newValue) {
        return diff(oldValue, newValue, DEFAULT_IGNORED_FIELDS);
    }

    /**
     * Compare two objects with custom ignored fields.
     *
     * @param oldValue the old object state
     * @param newValue the new object state
     * @param ignoredFields fields to ignore in comparison
     * @return map of field name to FieldChange (only changed fields)
     */
    public static Map<String, FieldChange> diff(Object oldValue, Object newValue, List<String> ignoredFields) {
        Map<String, FieldChange> changes = new LinkedHashMap<>();

        if (oldValue == null && newValue == null) {
            return changes;
        }

        if (oldValue == null || newValue == null) {
            changes.put(
                    "__object__",
                    new FieldChange(
                            oldValue == null ? null : safeToString(oldValue),
                            newValue == null ? null : safeToString(newValue)));
            return changes;
        }

        if (!oldValue.getClass().equals(newValue.getClass())) {
            log.warn(
                    "Cannot compare objects of different types: {} vs {}",
                    oldValue.getClass().getName(),
                    newValue.getClass().getName());
            return changes;
        }

        List<Field> fields = getAllFields(oldValue.getClass());
        for (Field field : fields) {
            if (ignoredFields.contains(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object oldFieldValue = field.get(oldValue);
                Object newFieldValue = field.get(newValue);

                if (!valuesEqual(oldFieldValue, newFieldValue)) {
                    changes.put(
                            field.getName(), new FieldChange(safeToString(oldFieldValue), safeToString(newFieldValue)));
                }
            } catch (IllegalAccessException e) {
                log.warn("Failed to access field: {}", field.getName(), e);
            }
        }

        return changes;
    }

    /**
     * Check if there are any changes between two objects.
     *
     * @param oldValue the old object state
     * @param newValue the new object state
     * @return true if there are changes
     */
    public static boolean hasChanges(Object oldValue, Object newValue) {
        return !diff(oldValue, newValue).isEmpty();
    }

    /**
     * Convert diff result to JSON string.
     *
     * @param changes the diff result
     * @return JSON string representation
     */
    public static String toJson(Map<String, FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize diff to JSON", e);
            return "{}";
        }
    }

    /**
     * Get a human-readable summary of changes.
     *
     * @param changes the diff result
     * @return human-readable summary
     */
    public static String toSummary(Map<String, FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return "No changes";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Changed ").append(changes.size()).append(" field(s): ");
        int i = 0;
        for (Map.Entry<String, FieldChange> entry : changes.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey());
            i++;
        }
        return sb.toString();
    }

    /**
     * Get all fields including inherited ones.
     *
     * @param clazz the class
     * @return list of all fields
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Compare two values for equality.
     *
     * @param o1 first value
     * @param o2 second value
     * @return true if equal
     */
    private static boolean valuesEqual(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }

    /**
     * Safely convert an object to string.
     *
     * @param value the object
     * @return string representation
     */
    private static String safeToString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String
                    || value instanceof Number
                    || value instanceof Boolean
                    || value instanceof Enum) {
                return value.toString();
            }
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * Represents a single field change.
     */
    public static class FieldChange {
        private final Object oldValue;
        private final Object newValue;

        public FieldChange(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        @Override
        public String toString() {
            return oldValue + " -> " + newValue;
        }
    }
}
