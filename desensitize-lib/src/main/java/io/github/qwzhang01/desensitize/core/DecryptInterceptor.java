/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.desensitize.core;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;

/**
 * MyBatis interceptor for automatic decryption of query results.
 *
 * <p>This interceptor intercepts {@link ResultSetHandler#handleResultSets} to automatically
 * decrypt encrypted fields in query results. It processes both single objects and lists,
 * finding fields annotated with {@link EncryptField} and applying the configured decryption
 * algorithm.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Automatic detection of encrypted fields via @EncryptField annotation</li>
 *   <li>Support for both selectOne and selectList operations</li>
 *   <li>Multiple encryption algorithm support through Strategy Pattern</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 *
 * <p><strong>Execution Flow:</strong></p>
 * <pre>
 * 1. Query executes and returns encrypted results
 * 2. Interceptor captures the result set
 * 3. Scans for @EncryptField annotated fields
 * 4. Applies appropriate decryption algorithm
 * 5. Returns decrypted data to caller
 * </pre>
 *
 * <p><strong>Performance Considerations:</strong></p>
 * <ul>
 *   <li>Uses reflection caching to minimize overhead</li>
 *   <li>Only processes fields with encryption annotations</li>
 *   <li>Skips processing if no encrypted fields are found</li>
 * </ul>
 *
 * @author avinzhang
 * @see EncryptField
 * @see AbstractEncryptAlgoContainer
 */
@Intercepts({
        @Signature(
                type = ResultSetHandler.class,
                method = "handleResultSets",
                args = {Statement.class}
        )
})
public class DecryptInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DecryptInterceptor.class);

    /**
     * Intercepts query result handling to decrypt encrypted fields.
     *
     * <p>This method is called after a query executes but before results are returned
     * to the application. It processes both single results (selectOne) and lists (selectList).</p>
     *
     * @param invocation the method invocation details
     * @return the result object with decrypted fields
     * @throws Throwable if the underlying operation fails
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Execute the original query
        Object resultObject = invocation.proceed();

        if (resultObject == null) {
            log.debug("Query returned null, skipping decryption");
            return null;
        }

        // Process results based on type
        if (resultObject instanceof List<?> resultList) {
            decryptListResults(resultList);
        } else {
            decryptSingleResult(resultObject);
        }

        return resultObject;
    }

    /**
     * Decrypts encrypted fields in a list of results (selectList scenario).
     *
     * @param resultList the list of query results
     */
    private void decryptListResults(List<?> resultList) {
        if (CollectionUtils.isEmpty(resultList)) {
            log.debug("Empty result list, skipping decryption");
            return;
        }

        log.debug("Decrypting {} results from list query", resultList.size());

        for (Object result : resultList) {
            if (result != null) {
                List<ClazzUtil.AnnotatedFieldResult<EncryptField>> encryptedFields =
                        ClazzUtil.getAnnotatedFields(result, EncryptField.class);
                decryptFields(encryptedFields);
            }
        }
    }

    /**
     * Decrypts encrypted fields in a single result (selectOne scenario).
     *
     * @param resultObject the query result object
     */
    private void decryptSingleResult(Object resultObject) {
        log.debug("Decrypting single result of type: {}", resultObject.getClass().getName());

        List<ClazzUtil.AnnotatedFieldResult<EncryptField>> encryptedFields =
                ClazzUtil.getAnnotatedFields(resultObject, EncryptField.class);
        decryptFields(encryptedFields);
    }

    /**
     * Decrypts a collection of encrypted fields using their configured algorithms.
     *
     * <p>This method retrieves the encryption container from Spring context and applies
     * the appropriate decryption algorithm to each field based on its annotation.</p>
     *
     * @param fields the list of annotated field results to decrypt
     * @throws DesensitizeException if decryption fails
     */
    private void decryptFields(List<ClazzUtil.AnnotatedFieldResult<EncryptField>> fields) {
        if (fields.isEmpty()) {
            log.debug("No encrypted fields found, skipping decryption");
            return;
        }

        AbstractEncryptAlgoContainer container = SpringContextUtil.getBean(AbstractEncryptAlgoContainer.class);
        if (container == null) {
            log.error("Encryption algorithm container not found in Spring context");
            throw new DesensitizeException("Encryption algorithm container not available");
        }

        log.debug("Decrypting {} encrypted fields", fields.size());

        try {
            for (ClazzUtil.AnnotatedFieldResult<EncryptField> fieldResult : fields) {
                decryptSingleField(fieldResult, container);
            }
        } catch (IllegalAccessException e) {
            throw new DesensitizeException("Failed to decrypt fields due to access error", e);
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
    private void decryptSingleField(ClazzUtil.AnnotatedFieldResult<EncryptField> fieldResult,
                                    AbstractEncryptAlgoContainer container) throws IllegalAccessException {
        EncryptField annotation = fieldResult.annotation();
        Field field = fieldResult.field();
        Object containingObject = fieldResult.containingObject();
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

        log.debug("Decrypted field: {} in object: {}",
                field.getName(), containingObject.getClass().getSimpleName());
    }

    /**
     * Wraps the target object with this interceptor.
     * Only wraps if the target is a ResultSetHandler.
     *
     * @param target the target object to potentially wrap
     * @return the wrapped target or the original target
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof ResultSetHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}

