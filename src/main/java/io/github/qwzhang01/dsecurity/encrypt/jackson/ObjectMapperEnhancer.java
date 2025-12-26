package io.github.qwzhang01.dsecurity.encrypt.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.qwzhang01.dsecurity.domain.Encrypt;
import io.github.qwzhang01.dsecurity.exception.JacksonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;

/**
 * ObjectMapper enhancer for Encrypt type serialization/deserialization.
 *
 * <p>This class automatically configures Jackson ObjectMapper to handle the
 * Encrypt type by registering custom serializers and deserializers when the
 * Spring context is refreshed.</p>
 *
 * <p><strong>Serialization:</strong> Encrypt objects are serialized as their
 * string values.</p>
 * <p><strong>Deserialization:</strong> String values are deserialized into
 * Encrypt objects.</p>
 *
 * @author avinzhang
 */
public class ObjectMapperEnhancer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log =
            LoggerFactory.getLogger(ObjectMapperEnhancer.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ObjectMapper bean =
                event.getApplicationContext().getBean(ObjectMapper.class);
        configureEncryptModule(bean);
    }


    /**
     * Configures Encrypt type serialization and deserialization.
     */
    private void configureEncryptModule(ObjectMapper objectMapper) {
        SimpleModule encryptModule = createEncryptModule();
        objectMapper.registerModule(encryptModule);
    }

    /**
     * Creates Encrypt serialization/deserialization module.
     */
    private SimpleModule createEncryptModule() {
        SimpleModule simpleModule = new SimpleModule("EncryptModule");

        // Serializer: Serializes Encrypt object to string
        simpleModule.addSerializer(Encrypt.class,
                new JsonSerializer<Encrypt>() {
                    @Override
                    public void serialize(Encrypt value, JsonGenerator g,
                                          SerializerProvider serializers) throws IOException {
                        if (value != null && value.getValue() != null) {
                            g.writeString(value.getValue());
                        } else {
                            g.writeNull();
                        }
                    }
                });

        // Deserializer: Deserializes string to Encrypt object
        simpleModule.addDeserializer(Encrypt.class,
                new JsonDeserializer<Encrypt>() {
                    @Override
                    public Encrypt deserialize(JsonParser p,
                                               DeserializationContext contextText) throws IOException {
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
                        throw new JacksonException("JSON deserialization error", "",
                                Encrypt.class);
                    }
                });

        return simpleModule;
    }
}