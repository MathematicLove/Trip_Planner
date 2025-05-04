package org.tripplanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Единое место, где регистрируем модули Jackson.
 * Spring-MVC автоматически подхватит этот ObjectMapper
 * для всех MappingJackson2HttpMessageConverter-ов.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                // Instant / LocalDateTime / ZoneId …
                .registerModule(new JavaTimeModule())
                // чтобы Java record-'ы/deserialization работали «из коробки»
                .registerModule(new ParameterNamesModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}
