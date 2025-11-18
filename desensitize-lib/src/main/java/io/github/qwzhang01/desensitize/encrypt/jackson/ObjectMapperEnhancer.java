package io.github.qwzhang01.desensitize.encrypt.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.exception.JacksonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;

public class ObjectMapperEnhancer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ObjectMapperEnhancer.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ObjectMapper bean = event.getApplicationContext().getBean(ObjectMapper.class);
        configureEncryptModule(bean);
    }


    /**
     * 配置 Encrypt 类型的序列化和反序列化
     */
    private void configureEncryptModule(ObjectMapper objectMapper) {
        SimpleModule encryptModule = createEncryptModule();
        objectMapper.registerModule(encryptModule);
    }

    /**
     * 创建 Encrypt 序列化/反序列化模块
     */
    private SimpleModule createEncryptModule() {
        SimpleModule simpleModule = new SimpleModule("EncryptModule");

        // 序列化器：将 Encrypt 对象序列化为字符串
        simpleModule.addSerializer(Encrypt.class, new JsonSerializer<Encrypt>() {
            @Override
            public void serialize(Encrypt value, JsonGenerator g, SerializerProvider serializers) throws IOException {
                if (value != null && value.getValue() != null) {
                    g.writeString(value.getValue());
                } else {
                    g.writeNull();
                }
            }
        });

        // 反序列化器：将字符串反序列化为 Encrypt 对象
        simpleModule.addDeserializer(Encrypt.class, new JsonDeserializer<Encrypt>() {
            @Override
            public Encrypt deserialize(JsonParser p, DeserializationContext contextText) throws IOException {
                int currentTokenId = p.currentTokenId();
                if (JsonTokenId.ID_STRING == currentTokenId) {
                    String text = p.getText().trim();
                    return new Encrypt(text);
                }
                JsonToken currentToken = p.getCurrentToken();
                if (JsonToken.VALUE_STRING == currentToken) {
                    String text = p.getText().trim();
                    return new Encrypt(text);
                }
                if (JsonToken.VALUE_NULL == currentToken) {
                    return null;
                }
                throw new JacksonException("json 反序列化异常", "", Encrypt.class);
            }
        });

        return simpleModule;
    }
}