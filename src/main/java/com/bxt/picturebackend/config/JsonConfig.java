package com.bxt.picturebackend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;

/**
 * Spring MVC Json 配置
 */
@JsonComponent
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置 + String 转 Long 的反序列化支持
     */
    @Bean
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 创建模块
        SimpleModule module = new SimpleModule();

        // 序列化：Long -> String
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 反序列化：String -> Long
        module.addDeserializer(Long.class, new JsonDeserializer<Long>() {
            @Override
            public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                if (value == null || value.isEmpty()) {
                    return null;
                }
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("无法将字符串解析为 Long 类型: " + value);
                }
            }
        });
        module.addDeserializer(Long.TYPE, new JsonDeserializer<Long>() {
            @Override
            public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                if (value == null || value.isEmpty()) {
                    return 0L;
                }
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("无法将字符串解析为 long 类型: " + value);
                }
            }
        });

        objectMapper.registerModule(module);
        return objectMapper;
    }
}
