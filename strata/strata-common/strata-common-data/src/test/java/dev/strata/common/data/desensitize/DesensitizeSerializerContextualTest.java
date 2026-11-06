// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.desensitize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link DesensitizeSerializer#createContextual} 的上下文解析分支：无 property、非 String 类型、无
 * {@link Desensitize} 注解三条回退路径，以及 repeat(0) 边界。
 */
@DisplayName("DesensitizeSerializer 上下文解析")
@SuppressWarnings({"rawtypes", "unchecked"})
class DesensitizeSerializerContextualTest {

    @Test
    @DisplayName("property 为 null 时回退为 null-value 序列化器")
    void nullPropertyFallsBackToNullValueSerializer() throws Exception {
        SerializerProvider prov = mock(SerializerProvider.class);
        JsonSerializer nullValueSer = mock(JsonSerializer.class);
        when(prov.findNullValueSerializer(null)).thenReturn(nullValueSer);

        JsonSerializer result = new DesensitizeSerializer().createContextual(prov, null);

        assertThat(result).isSameAs(nullValueSer);
    }

    @Test
    @DisplayName("非 String 字段类型回退为该类型的默认序列化器")
    void nonStringTypeFallsBackToTypeSerializer() throws Exception {
        SerializerProvider prov = mock(SerializerProvider.class);
        BeanProperty property = mock(BeanProperty.class);
        JavaType type = mock(JavaType.class);
        when(property.getType()).thenReturn(type);
        when(type.getRawClass()).thenReturn((Class) Integer.class);
        JsonSerializer typeSer = mock(JsonSerializer.class);
        // createContextual 回退分支调用 findValueSerializer(JavaType, BeanProperty)
        when(prov.findValueSerializer(type, property)).thenReturn(typeSer);

        JsonSerializer result = new DesensitizeSerializer().createContextual(prov, property);

        assertThat(result).isSameAs(typeSer);
    }

    @Test
    @DisplayName("String 字段但无 @Desensitize 注解时回退为 String 默认序列化器")
    void stringPropertyWithoutAnnotationFallsBack() throws Exception {
        SerializerProvider prov = mock(SerializerProvider.class);
        BeanProperty property = mock(BeanProperty.class);
        JavaType type = mock(JavaType.class);
        when(property.getType()).thenReturn(type);
        when(type.getRawClass()).thenReturn((Class) String.class);
        when(property.getAnnotation(Desensitize.class)).thenReturn(null);
        when(property.getContextAnnotation(Desensitize.class)).thenReturn(null);
        JsonSerializer stringSer = mock(JsonSerializer.class);
        when(prov.findValueSerializer(type, property)).thenReturn(stringSer);

        JsonSerializer result = new DesensitizeSerializer().createContextual(prov, property);

        assertThat(result).isSameAs(stringSer);
    }

    @Test
    @DisplayName("7 位手机号使 repeat(0) 走空串分支")
    void sevenDigitPhoneHitsZeroRepeat() throws IOException {
        DesensitizeSerializer serializer = new DesensitizeSerializer(DesensitizeType.PHONE, 0, 0, '*');
        JsonGenerator gen = mock(JsonGenerator.class);

        serializer.serialize("1234567", gen, null);

        // 123 + repeat(0) + 4567 = "1234567"
        verify(gen).writeString("1234567");
    }
}
