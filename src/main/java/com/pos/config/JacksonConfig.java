package com.pos.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter LOCAL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
                .serializerByType(java.time.LocalDateTime.class, new LocalDateTimeSerializer(LOCAL_DATETIME_FORMATTER))
                .deserializerByType(java.time.LocalDateTime.class, new LocalDateTimeDeserializer(LOCAL_DATETIME_FORMATTER))
                .serializerByType(java.time.LocalDate.class, new LocalDateSerializer(LOCAL_DATE_FORMATTER))
                .deserializerByType(java.time.LocalDate.class, new LocalDateDeserializer(LOCAL_DATE_FORMATTER));
    }
}
