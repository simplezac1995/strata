// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;
import java.util.Objects;

/**
 * Jackson serializer that applies desensitization to fields annotated with {@link Desensitize}.
 *
 * <p>Automatically masks sensitive data based on the configured {@link DesensitizeType}.
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;
    private int keepLeft;
    private int keepRight;
    private char maskChar;

    public DesensitizeSerializer() {}

    public DesensitizeSerializer(DesensitizeType type, int keepLeft, int keepRight, char maskChar) {
        this.type = type;
        this.keepLeft = keepLeft;
        this.keepRight = keepRight;
        this.maskChar = maskChar;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.isEmpty()) {
            gen.writeString(value);
            return;
        }
        gen.writeString(desensitize(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        if (Objects.equals(property.getType().getRawClass(), String.class)) {
            Desensitize desensitize = property.getAnnotation(Desensitize.class);
            if (desensitize == null) {
                desensitize = property.getContextAnnotation(Desensitize.class);
            }
            if (desensitize != null) {
                return new DesensitizeSerializer(
                        desensitize.type(), desensitize.keepLeft(), desensitize.keepRight(), desensitize.maskChar());
            }
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    private String desensitize(String value) {
        return switch (type) {
            case PHONE -> desensitizePhone(value);
            case EMAIL -> desensitizeEmail(value);
            case ID_CARD -> desensitizeIdCard(value);
            case BANK_CARD -> desensitizeBankCard(value);
            case NAME -> desensitizeName(value);
            case ADDRESS -> desensitizeAddress(value);
            case PASSWORD -> "******";
            case FULL_MASK -> maskAll(value);
            case CUSTOM -> desensitizeCustom(value, keepLeft, keepRight);
        };
    }

    private String desensitizePhone(String phone) {
        if (phone.length() < 7) {
            return maskAll(phone);
        }
        return phone.substring(0, 3) + repeat(maskChar, phone.length() - 7) + phone.substring(phone.length() - 4);
    }

    private String desensitizeEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return maskAll(email);
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (username.length() <= 2) {
            return username.charAt(0) + repeat(maskChar, 3) + domain;
        }
        return username.charAt(0) + repeat(maskChar, username.length() - 1) + domain;
    }

    private String desensitizeIdCard(String idCard) {
        if (idCard.length() < 10) {
            return maskAll(idCard);
        }
        return idCard.substring(0, 6) + repeat(maskChar, idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    private String desensitizeBankCard(String bankCard) {
        String cleaned = bankCard.replaceAll("\\s", "");
        if (cleaned.length() < 8) {
            return maskAll(bankCard);
        }
        StringBuilder result = new StringBuilder();
        result.append(cleaned, 0, 4);
        for (int i = 4; i < cleaned.length() - 4; i++) {
            if (i % 4 == 0) {
                result.append(' ');
            }
            result.append(maskChar);
        }
        result.append(' ').append(cleaned.substring(cleaned.length() - 4));
        return result.toString();
    }

    private String desensitizeName(String name) {
        if (name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + repeat(maskChar, name.length() - 1);
    }

    private String desensitizeAddress(String address) {
        if (address.length() <= 6) {
            return maskAll(address);
        }
        return address.substring(0, 6) + repeat(maskChar, address.length() - 6);
    }

    private String desensitizeCustom(String value, int left, int right) {
        if (value.length() <= left + right) {
            return maskAll(value);
        }
        return value.substring(0, left)
                + repeat(maskChar, value.length() - left - right)
                + value.substring(value.length() - right);
    }

    private String maskAll(String value) {
        return repeat(maskChar, value.length());
    }

    private String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
