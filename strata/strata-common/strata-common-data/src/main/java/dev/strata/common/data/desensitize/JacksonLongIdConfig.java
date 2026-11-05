// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Snowflake ids are 64-bit longs that routinely exceed JS's safe-integer range (2^53). Serializing them as JSON
 * numbers makes the browser parse them as doubles and lose precision, so a echoed-back path id no longer matches
 * the database row (e.g. player CS tickets returning "not found"). We stringify only id-typed Long properties
 * (named {@code id} or ending in {@code Id}); plain counts/amounts/{@code totalElements} stay numeric.
 */
@Configuration
public class JacksonLongIdConfig {

    @Bean
    public SimpleModule longIdAsStringModule() {
        SimpleModule module = new SimpleModule("long-id-as-string");
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(
                    SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                for (BeanPropertyWriter writer : beanProperties) {
                    if (isLongIdProperty(writer)) {
                        writer.assignSerializer(new ToStringLongSerializer());
                    }
                }
                return beanProperties;
            }
        });
        return module;
    }

    private static boolean isLongIdProperty(BeanPropertyWriter writer) {
        Class<?> type = writer.getType().getRawClass();
        if (type != Long.class && type != long.class) {
            return false;
        }
        String name = writer.getName();
        return name.equals("id") || name.endsWith("Id");
    }

    /** Serializes a long as its decimal string form without precision loss. */
    private static final class ToStringLongSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }
}
