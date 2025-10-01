package com.qw.desensitize.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

/**
 * jackson 配置
 *
 * @author avinzhang
 */
@Configuration
public class ObjectMapperConfig {
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        //不区分大小写设置
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        //对象的所有字段全部列入
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //忽略空Bean转json的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //取消默认转换timestamps形式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        //忽略在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // 默认启用，通常无需显式设置
        objectMapper.enable(MapperFeature.USE_ANNOTATIONS);

        SimpleModule defaultModule = new JavaTimeModule();
        objectMapper.registerModule(defaultModule);

        // double json 格式化
        SimpleModule doubleModule = new SimpleModule();
        doubleModule.addSerializer(Double.class, DoubleFormatSerializer.INSTANCE);
        objectMapper.registerModule(doubleModule);
        return objectMapper;
    }

    /**
     * double json 格式化
     * 保留2位小数
     *
     * @author avinzhang
     */
    private static final class DoubleFormatSerializer extends JsonSerializer<Double> {

        private static final DoubleFormatSerializer INSTANCE = new DoubleFormatSerializer();
        private static final DecimalFormat FORMAT = new DecimalFormat("###.##");

        @Override
        public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            String text = null;
            if (value != null) {
                try {
                    text = FORMAT.format(value);
                } catch (Exception e) {
                    text = value.toString();
                }
            }
            if (text != null) {
                jsonGenerator.writeString(text);
            }
        }
    }
}