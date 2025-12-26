package io.github.qwzhang01.desensitize.encrypt.processor;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import io.github.qwzhang01.desensitize.domain.AnnotatedField;
import io.github.qwzhang01.desensitize.encrypt.annotation.EncryptField;
import io.github.qwzhang01.desensitize.encrypt.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.encrypt.shield.EncryptionAlgo;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Decryption processor for automatic field decryption.
 *
 * <p>This processor handles automatic decryption of encrypted fields in query
 * results. It uses reflection to find fields annotated with {@link EncryptField}
 * and applies the configured decryption algorithm.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Singleton pattern for performance</li>
 *   <li>Support for both single and list results</li>
 *   <li>Automatic algorithm selection based on annotation</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 *
 * @author avinzhang
 */
public class DecryptProcessor {
    private static final Logger log =
            LoggerFactory.getLogger(DecryptProcessor.class);

    private DecryptProcessor() {
    }

    public static DecryptProcessor getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Decrypts encrypted fields in a list of results (selectList scenario).
     *
     * @param resultList the list of query results
     */
    public void decryptList(List<?> resultList) {
        if (CollectionUtils.isEmpty(resultList)) {
            log.debug("Empty result list, skipping decryption");
            return;
        }

        log.debug("Decrypting {} results from list query", resultList.size());

        for (Object result : resultList) {
            if (result != null) {
                List<AnnotatedField<EncryptField>> encryptedFields =
                        ClazzUtil.getAnnotatedFields(result,
                                EncryptField.class);
                decryptFields(encryptedFields);
            }
        }
    }

    /**
     * Decrypts encrypted fields in a single result (selectOne scenario).
     *
     * @param resultObject the query result object
     */
    public void decryptSingle(Object resultObject) {
        log.debug("Decrypting single result of type: {}",
                resultObject.getClass().getName());

        List<AnnotatedField<EncryptField>> encryptedFields =
                ClazzUtil.getAnnotatedFields(resultObject, EncryptField.class);
        decryptFields(encryptedFields);
    }

    /**
     * Decrypts a collection of encrypted fields using their configured
     * algorithms.
     *
     * <p>This method retrieves the encryption container from Spring context
     * and applies
     * the appropriate decryption algorithm to each field based on its
     * annotation.</p>
     *
     * @param fields the list of annotated field results to decrypt
     * @throws DesensitizeException if decryption fails
     */
    private void decryptFields(List<AnnotatedField<EncryptField>> fields) {
        if (fields.isEmpty()) {
            log.debug("No encrypted fields found, skipping decryption");
            return;
        }

        AbstractEncryptAlgoContainer container =
                SpringContextUtil.getBean(AbstractEncryptAlgoContainer.class);
        if (container == null) {
            log.error("Encryption algorithm container not found in Spring " +
                    "context");
            throw new DesensitizeException("Encryption algorithm container " +
                    "not available");
        }

        log.debug("Decrypting {} encrypted fields", fields.size());

        try {
            for (AnnotatedField<EncryptField> fieldResult : fields) {
                decryptSingleField(fieldResult, container);
            }
        } catch (IllegalAccessException e) {
            throw new DesensitizeException("Failed to decrypt fields due to " +
                    "access error", e);
        } catch (Exception e) {
            throw new DesensitizeException("Failed to decrypt fields", e);
        }
    }

    /**
     * Decrypts a single field value using the configured encryption algorithm.
     *
     * @param fieldResult the annotated field result containing field metadata
     * @param container   the encryption algorithm container
     * @throws IllegalAccessException if field access fails
     */
    private void decryptSingleField(AnnotatedField<EncryptField> fieldResult,
                                    AbstractEncryptAlgoContainer container) throws IllegalAccessException {
        EncryptField annotation = fieldResult.annotation();
        Field field = fieldResult.field();
        Object containingObject = fieldResult.obj();
        Object value = fieldResult.getFieldValue();

        // Only decrypt String values
        if (!(value instanceof String strValue)) {
            log.debug("Skipping non-String field: {}", field.getName());
            return;
        }

        // Apply decryption using the configured algorithm
        Class<? extends EncryptionAlgo> algoClass = annotation.value();
        String decryptedValue = container.getAlgo(algoClass).decrypt(strValue);

        // Update the field with decrypted value
        field.setAccessible(true);
        field.set(containingObject, decryptedValue);

        log.debug("Decrypted field: {} in object: {}", field.getName(),
                containingObject.getClass().getSimpleName());
    }

    private static final class Holder {
        private static final DecryptProcessor INSTANCE = new DecryptProcessor();
    }
}
