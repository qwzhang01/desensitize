package io.github.qwzhang01.desensitize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.qwzhang01.desensitize.encrypt.jackson.ObjectMapperEnhancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper configuration for the desensitization library.
 *
 * <p>This configuration class sets up Jackson JSON processing with support for:</p>
 * <ul>
 *   <li>Enhanced ObjectMapper with Encrypt type serialization/deserialization</li>
 *   <li>Java 8 date/time types (LocalDateTime, LocalDate, etc.)</li>
 *   <li>Human-readable date formatting instead of timestamps</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {
    
    /**
     * Creates an ObjectMapperEnhancer bean that configures Encrypt type handling.
     * This enhancer automatically registers custom serializers and deserializers
     * for the Encrypt type when the Spring context is refreshed.
     *
     * @return the ObjectMapperEnhancer instance
     */
    @Bean
    public ObjectMapperEnhancer objectMapperEnhancer() {
        return new ObjectMapperEnhancer();
    }

    /**
     * Creates a default ObjectMapper bean only if no other ObjectMapper is defined.
     * This prevents conflicts with user-defined ObjectMapper configurations.
     *
     * <p>The default configuration includes:</p>
     * <ul>
     *   <li>JavaTimeModule for Java 8 date/time support</li>
     *   <li>Disabled timestamp serialization for dates (uses ISO-8601 format instead)</li>
     * </ul>
     *
     * @return a configured ObjectMapper instance
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register JavaTimeModule for Java 8 date/time types support
        objectMapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 format instead)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Encrypt type serialization and deserialization will be configured
        // by ObjectMapperEnhancer

        return objectMapper;
    }
}