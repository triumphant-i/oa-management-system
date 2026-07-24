package com.southwind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Configuration
public class JacksonConfig {

    // 序列化使用ISO格式（带T），反序列化支持多种格式
    private static final DateTimeFormatter SERIALIZE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter[] DESERIALIZE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),  // ISO格式
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),     // 空格格式
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),        // 不带秒的空格格式
        DateTimeFormatter.ISO_LOCAL_DATE_TIME                   // 标准ISO格式
    };

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        
        // 序列化使用ISO格式
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(SERIALIZE_FORMATTER));
        
        // 反序列化支持多种格式
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer() {
            @Override
            public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser, 
                    com.fasterxml.jackson.databind.DeserializationContext context) 
                    throws java.io.IOException {
                String text = parser.getText().trim();
                for (DateTimeFormatter formatter : DESERIALIZE_FORMATTERS) {
                    try {
                        return LocalDateTime.parse(text, formatter);
                    } catch (Exception e) {
                        // 尝试下一个格式
                    }
                }
                // 如果都失败，使用默认行为
                return super.deserialize(parser, context);
            }
        });
        
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}