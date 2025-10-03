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


package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.annotation.Mask;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultCoverAlgo;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for managing masking algorithm instances and providing data masking functionality.
 * Provides lazy initialization and caching to avoid circular dependency issues.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public final class MaskAlgoContainer {

    /**
     * Cache for algorithm instances to avoid repeated creation
     */
    private static final ConcurrentHashMap<Class<? extends CoverAlgo>, CoverAlgo> ALGO_CACHE = new ConcurrentHashMap<>();

    /**
     * Gets a masking algorithm instance by class type.
     * First tries to get from Spring context, then falls back to direct instantiation.
     *
     * @param clazz the masking algorithm class
     * @return the masking algorithm instance
     */
    public static CoverAlgo getAlgo(Class<? extends CoverAlgo> clazz) {
        return ALGO_CACHE.computeIfAbsent(clazz, key -> {
            // First try to get from Spring context if available
            if (SpringContextUtil.isInitialized()) {
                CoverAlgo algo = SpringContextUtil.getBeanSafely(key);
                if (algo != null) {
                    return algo;
                }
            }

            // Fallback to direct instantiation
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // If the requested class fails, try default algorithm
                if (!key.equals(DefaultCoverAlgo.class)) {
                    try {
                        return new DefaultCoverAlgo();
                    } catch (Exception ex) {
                        throw new DesensitizeException("Failed to create masking algorithm instance", ex);
                    }
                }
                throw new DesensitizeException("Failed to create masking algorithm instance", e);
            }
        });
    }

    /**
     * Clears the algorithm cache. Useful for testing or when algorithms need to be reloaded.
     */
    public static void clearCache() {
        ALGO_CACHE.clear();
    }

    /**
     * Masks sensitive data in the given object recursively.
     * Processes both single objects and lists of objects.
     *
     * @param data the data object to mask
     * @return the masked data object
     */
    public Object mask(Object data) {
        try {
            if (data instanceof List<?> list) {
                // Process list
                for (Object o : list) {
                    mask(o);
                }
            } else {
                // Process single object
                maskParam(data);
            }
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new DesensitizeException("Failed to mask data", e);
        }
        return data;
    }

    /**
     * Masks fields in a single object based on @Mask annotations.
     * Recursively processes nested objects and collections.
     *
     * @param data the object to process for masking
     * @throws IllegalAccessException    if field access fails
     * @throws InstantiationException    if algorithm instantiation fails
     * @throws NoSuchMethodException     if constructor is not found
     * @throws InvocationTargetException if constructor invocation fails
     */
    private void maskParam(Object data) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (data == null) {
            return;
        }

        List<ClazzUtil.AnnotatedFieldResult<Mask>> maskFields = ClazzUtil.getAnnotatedAnnotationFields(data, Mask.class);
        if (CollectionUtils.isEmpty(maskFields)) {
            return;
        }

        for (ClazzUtil.AnnotatedFieldResult<Mask> maskField : maskFields) {
            Mask annotation = maskField.annotation();
            Object object = maskField.containingObject();
            Object fieldValue = maskField.getFieldValue();
            Field field = maskField.field();

            Class<? extends CoverAlgo> clazz = annotation.value();
            CoverAlgo coverAlgo = getAlgo(clazz);
            if (fieldValue != null) {
                Class<?> type = field.getType();
                if (String.class.equals(type)) {
                    field.set(object, coverAlgo.mask(String.valueOf(fieldValue)));
                } else if (Encrypt.class.equals(type)) {
                    Encrypt encrypt = (Encrypt) fieldValue;
                    encrypt.setValue(coverAlgo.mask(encrypt.getValue()));
                    field.set(object, encrypt);
                } else {
                    // todo 处理集合等其他情况
                }
            }
        }
    }
}