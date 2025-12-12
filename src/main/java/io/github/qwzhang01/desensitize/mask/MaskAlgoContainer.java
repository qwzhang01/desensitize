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


package io.github.qwzhang01.desensitize.mask;

import io.github.qwzhang01.desensitize.domain.AnnotatedField;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.mask.annotation.Mask;
import io.github.qwzhang01.desensitize.mask.domain.MaskVo;
import io.github.qwzhang01.desensitize.mask.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.mask.shield.DefaultCoverAlgo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for managing masking algorithm instances and providing data masking functionality.
 * Uses Factory Pattern combined with Strategy Pattern to provide flexible masking capabilities.
 *
 * <p>This container provides:</p>
 * <ul>
 *   <li>Thread-safe lazy instantiation with caching</li>
 *   <li>Spring context integration for dependency injection</li>
 *   <li>Automatic fallback to default algorithm on failures</li>
 *   <li>Recursive masking for complex object structures</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public final class MaskAlgoContainer {

    private static final Logger log = LoggerFactory.getLogger(MaskAlgoContainer.class);

    /**
     * Cache for algorithm instances to avoid repeated creation.
     * Uses ConcurrentHashMap for thread-safe operations.
     */
    private static final ConcurrentHashMap<Class<? extends CoverAlgo>, CoverAlgo> ALGO_CACHE
            = new ConcurrentHashMap<>();

    /**
     * Clears the algorithm cache.
     * Useful for testing scenarios or when algorithms need to be reloaded at runtime.
     */
    public static void clearCache() {
        log.debug("Clearing masking algorithm cache");
        ALGO_CACHE.clear();
    }

    /**
     * Gets a masking algorithm instance by class type using Factory Pattern.
     *
     * <p>The algorithm retrieval follows this priority:</p>
     * <ol>
     *   <li>Return cached instance if available</li>
     *   <li>Try to get bean from Spring context (supports dependency injection)</li>
     *   <li>Create new instance via reflection</li>
     *   <li>Fallback to default algorithm on failure</li>
     * </ol>
     *
     * @param clazz the masking algorithm class
     * @return the masking algorithm instance, never null
     * @throws DesensitizeException if algorithm cannot be created and no fallback is available
     */
    private CoverAlgo getAlgo(Class<? extends CoverAlgo> clazz) {
        if (clazz == null) {
            log.warn("Null algorithm class provided, using default algorithm");
            return new DefaultCoverAlgo();
        }

        return ALGO_CACHE.computeIfAbsent(clazz, this::createAlgorithmInstance);
    }

    /**
     * Creates a new algorithm instance with fallback mechanism.
     *
     * @param clazz the algorithm class to instantiate
     * @return the created algorithm instance
     * @throws DesensitizeException if creation fails and no fallback is available
     */
    private CoverAlgo createAlgorithmInstance(Class<? extends CoverAlgo> clazz) {
        log.debug("Creating new instance of masking algorithm: {}", clazz.getName());

        // Strategy 1: Try to get from Spring context (supports dependency injection)
        if (SpringContextUtil.isInitialized()) {
            CoverAlgo algo = SpringContextUtil.getBeanSafely(clazz);
            if (algo != null) {
                log.debug("Retrieved masking algorithm from Spring context: {}", clazz.getName());
                return algo;
            }
        }

        // Strategy 2: Try direct instantiation via reflection
        try {
            CoverAlgo instance = clazz.getDeclaredConstructor().newInstance();
            log.debug("Successfully created masking algorithm instance: {}", clazz.getName());
            return instance;
        } catch (Exception e) {
            log.error("Failed to instantiate masking algorithm: {}", clazz.getName(), e);
            return handleInstantiationFailure(clazz, e);
        }
    }

    /**
     * Handles algorithm instantiation failure with fallback mechanism.
     *
     * @param clazz the algorithm class that failed to instantiate
     * @param cause the exception that caused the failure
     * @return the fallback algorithm instance
     * @throws DesensitizeException if fallback also fails
     */
    private CoverAlgo handleInstantiationFailure(Class<? extends CoverAlgo> clazz, Exception cause) {
        // If not already trying the default algorithm, fall back to it
        if (!clazz.equals(DefaultCoverAlgo.class)) {
            log.warn("Falling back to default masking algorithm due to instantiation failure");
            try {
                return new DefaultCoverAlgo();
            } catch (Exception ex) {
                throw new DesensitizeException(
                        "Failed to create masking algorithm instance and fallback also failed", ex);
            }
        }

        // If default algorithm itself fails, throw exception
        throw new DesensitizeException(
                "Failed to create default masking algorithm instance", cause);
    }

    /**
     * Masks sensitive data in the given object recursively.
     *
     * <p>This method processes data objects and applies masking algorithms to fields
     * annotated with {@link Mask} or meta-annotations containing {@link Mask}.</p>
     *
     * <p><strong>Supported Data Types:</strong></p>
     * <ul>
     *   <li>Single objects with @Mask annotated fields</li>
     *   <li>Lists of objects (each element is processed recursively)</li>
     *   <li>Nested objects (automatically traversed)</li>
     * </ul>
     *
     * @param data the data object to mask, can be null
     * @return the same object instance with masked fields, or null if input is null
     * @throws DesensitizeException if masking fails due to reflection or algorithm errors
     */
    public Object mask(Object data) {
        if (data == null) {
            return null;
        }

        try {
            if (data instanceof List<?> list) {
                maskList(list);
            } else {
                maskSingleObject(data);
            }

            return data;
        } catch (IllegalAccessException e) {
            throw new DesensitizeException("Failed to mask data due to field access error", e);
        } catch (Exception e) {
            throw new DesensitizeException("Failed to mask data", e);
        }
    }

    private void maskList(List<?> list) throws IllegalAccessException {
        log.debug("Masking list with {} elements", list.size());
        for (Object data : list) {
            if (data instanceof List<?> listInner) {
                maskList(listInner);
            } else {
                maskSingleObject(data);
            }
        }
    }

    /**
     * Masks fields in a single object based on @Mask annotations.
     *
     * <p>This method uses reflection to find and mask annotated fields.
     * It supports both direct @Mask annotations and meta-annotations.</p>
     *
     * @param data the object to process for masking, can be null
     * @throws IllegalAccessException if field access fails
     */
    private void maskSingleObject(Object data) throws IllegalAccessException {
        if (data == null) {
            return;
        }

        Set<String> maskFieldSet = null;
        if (data instanceof MaskVo maskVo) {
            if (!Boolean.TRUE.equals(maskVo.getMaskFlag())) {
                return;
            }
            List<String> maskFieldName = maskVo.getMaskFields();
            if (maskFieldName != null && !maskFieldName.isEmpty()) {
                maskFieldSet = new HashSet<>(maskFieldName);
            }
        }

        // Find all fields with @Mask annotation (including meta-annotations)
        List<AnnotatedField<Mask>> maskFields =
                ClazzUtil.getMetaAnnotatedFields(data, Mask.class);

        if (CollectionUtils.isEmpty(maskFields)) {
            log.debug("No fields to mask in object of type: {}", data.getClass().getName());
            return;
        }

        log.debug("Masking {} fields in object of type: {}", maskFields.size(), data.getClass().getName());

        for (AnnotatedField<Mask> maskField : maskFields) {
            if (maskFieldSet == null) {
                maskSingleField(maskField);
            } else {
                if (maskFieldSet.contains(maskField.fieldPath())) {
                    maskSingleField(maskField);
                }
            }

        }
    }

    /**
     * Masks a single field based on its @Mask annotation configuration.
     *
     * <p>Supported field types:</p>
     * <ul>
     *   <li>{@link String} - directly masked</li>
     *   <li>{@link Encrypt} - value inside Encrypt object is masked</li>
     *   <li>Other types - currently logged and skipped (can be extended)</li>
     * </ul>
     *
     * @param maskField the annotated field result containing field, object, and annotation
     * @throws IllegalAccessException if field access fails
     */
    private void maskSingleField(AnnotatedField<Mask> maskField) throws IllegalAccessException {
        Mask annotation = maskField.annotation();
        Object containingObject = maskField.obj();
        Object fieldValue = maskField.getFieldValue();
        Field field = maskField.field();

        if (fieldValue == null) {
            log.debug("Skipping null field: {}", field.getName());
            return;
        }

        // Get the appropriate masking algorithm
        Class<? extends CoverAlgo> algoClass = annotation.value();
        CoverAlgo coverAlgo = getAlgo(algoClass);

        Class<?> fieldType = field.getType();

        // Apply masking based on field type
        if (String.class.equals(fieldType)) {
            String maskedValue = coverAlgo.mask(String.valueOf(fieldValue));
            field.set(containingObject, maskedValue);
            log.debug("Masked String field: {}", field.getName());

        } else if (Encrypt.class.equals(fieldType)) {
            Encrypt encrypt = (Encrypt) fieldValue;
            String maskedValue = coverAlgo.mask(encrypt.getValue());
            encrypt.setValue(maskedValue);
            field.set(containingObject, encrypt);
            log.debug("Masked Encrypt field: {}", field.getName());

        } else {
            // TODO: Support for collections and other complex types
            log.debug("Unsupported field type for masking: {} (field: {})",
                    fieldType.getName(), field.getName());
        }
    }

    /**
     * Gets the current size of the algorithm cache.
     * Useful for monitoring and debugging purposes.
     *
     * @return the number of cached algorithm instances
     */
    public int getCacheSize() {
        return ALGO_CACHE.size();
    }
}