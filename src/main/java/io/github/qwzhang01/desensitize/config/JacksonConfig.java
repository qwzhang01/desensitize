package io.github.qwzhang01.desensitize.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.qwzhang01.desensitize.encrypt.jackson.ObjectMapperEnhancer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapperEnhancer objectMapperEnhancer() {
        return new ObjectMapperEnhancer();
    }

    /**
     * 仅在没有 ObjectMapper 时创建默认的 ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 注册 JavaTimeModule
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置 Encrypt 类型的序列化和反序列化

        return objectMapper;
    }
}