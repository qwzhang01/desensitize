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
 * The above copyright notice and this permission notice shall be included in
 *  all
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

import io.github.qwzhang01.desensitize.domain.AnnotatedField;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Class operation utility for reflection operations.
 *
 * @author avinzhang
 */
public final class ClazzUtil {
    private final static Set<Class<?>> NO_CLASS = new CopyOnWriteArraySet<>();
    private static final Map<Class<?>, List<Field>> FIELD_CACHE =
            new ConcurrentHashMap<>();
    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Byte.class, Short.class, Character.class,
            int.class, long.class, double.class, float.class,
            boolean.class, byte.class, short.class, char.class
    );

    /**
     * Get object property value
     */
    public static Object getPropertyValue(Object obj, String propertyName) throws Exception {
        if (obj == null || propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Object and property name " +
                    "cannot be null");
        }

        if (isPrimitiveOrCommonType(obj.getClass())) {
            return obj;
        }

        String capitalizedName = StringUtils.capitalize(propertyName);

        Object result = tryGetterMethod(obj, "get" + capitalizedName);
        if (result != null) {
            return result;
        }

        result = tryGetterMethod(obj, "is" + capitalizedName);
        if (result != null) {
            return result;
        }

        return getFieldValue(obj, propertyName);
    }

    /**
     * Set object property value
     */
    public static void setPropertyValue(Object obj, String propertyName,
                                        Object value) throws Exception {
        if (obj == null || propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Object and property name " +
                    "cannot be null");
        }

        String setterName = "set" + StringUtils.capitalize(propertyName);

        if (trySetterMethod(obj, setterName, value)) {
            return;
        }

        setFieldValue(obj, propertyName, value);
    }

    /**
     * Try getter method
     */
    private static Object tryGetterMethod(Object obj, String methodName) {
        try {
            Method getter = obj.getClass().getMethod(methodName);
            return getter.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Try setter method
     */
    private static boolean trySetterMethod(Object obj, String methodName,
                                           Object value) {
        try {
            Method setter = obj.getClass().getMethod(methodName,
                    value != null ? value.getClass() : Object.class);
            setter.invoke(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get value via field
     */
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new DesensitizeException("Cannot get property: " + fieldName);
        }
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * Set value via field
     */
    private static void setFieldValue(Object obj, String fieldName,
                                      Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new DesensitizeException("Cannot set property: " + fieldName);
        }
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * Find method in class hierarchy
     */
    public static Method findMethod(Class<?> clazz, String methodName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Find field in class hierarchy
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Get fields with meta-annotations
     */
    public static <T extends Annotation> List<AnnotatedField<T>> getMetaAnnotatedFields(
            Object obj, Class<T> annotationClass) {
        return getAnnotatedFieldsInternal(obj, annotationClass, true);
    }

    /**
     * Get fields with direct annotations
     */
    public static <T extends Annotation> List<AnnotatedField<T>> getAnnotatedFields(
            Object obj, Class<T> annotationClass) {
        return getAnnotatedFieldsInternal(obj, annotationClass, false);
    }

    /**
     * Internal method for retrieving annotated fields
     */
    private static <T extends Annotation> List<AnnotatedField<T>> getAnnotatedFieldsInternal(
            Object obj, Class<T> annotationClass,
            boolean searchMetaAnnotation) {
        if (obj == null || annotationClass == null) {
            return Collections.emptyList();
        }

        if (NO_CLASS.contains(obj.getClass())) {
            return Collections.emptyList();
        }

        List<AnnotatedField<T>> results = new ArrayList<>();
        Set<Object> visited = new HashSet<>();

        collectAnnotatedFields(obj, annotationClass, results, visited, "",
                searchMetaAnnotation);

        if (results.isEmpty() && !isCollection(obj.getClass()) && !isGenerics(obj.getClass())) {
            NO_CLASS.add(obj.getClass());
        }

        return results;
    }

    /**
     * Collect annotated fields recursively
     */
    private static <T extends Annotation> void collectAnnotatedFields(
            Object obj,
            Class<T> annotationClass,
            List<AnnotatedField<T>> results,
            Set<Object> visited,
            String fieldPath,
            boolean searchMetaAnnotation) {

        if (obj == null || visited.contains(obj)) {
            return;
        }

        Class<?> objClass = obj.getClass();

        if (isPrimitiveOrCommonType(objClass)) {
            return;
        }

        visited.add(obj);

        try {
            List<Field> fields = getAllFields(objClass);

            for (Field field : fields) {
                if (isFinalAndStatic(field)) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    T annotation = findAnnotation(field, annotationClass,
                            searchMetaAnnotation);
                    if (annotation != null) {
                        if (searchMetaAnnotation) {
                            Class<?> type = field.getType();
                            if (isComplexObject(type)) {
                                Object fieldValue = field.get(obj);
                                String currentPath = buildFieldPath(fieldPath
                                        , field.getName());
                                if (fieldValue != null) {
                                    processComplexFieldValue(fieldValue,
                                            annotationClass, results, visited
                                            , currentPath,
                                            searchMetaAnnotation);
                                }
                            } else {
                                String currentPath = buildFieldPath(fieldPath
                                        , field.getName());
                                results.add(new AnnotatedField<>(field, obj,
                                        annotation, currentPath));
                            }
                        } else {
                            String currentPath = buildFieldPath(fieldPath,
                                    field.getName());
                            results.add(new AnnotatedField<>(field, obj,
                                    annotation, currentPath));
                        }
                    }

                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && isComplexObject(fieldValue.getClass())) {
                        String currentPath = buildFieldPath(fieldPath,
                                field.getName());
                        processComplexFieldValue(fieldValue, annotationClass,
                                results, visited, currentPath,
                                searchMetaAnnotation);
                    }

                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }
        } finally {
            visited.remove(obj);
        }
    }

    /**
     * Find annotation on field
     */
    private static <T extends Annotation> T findAnnotation(Field field,
                                                           Class<T> annotationClass, boolean searchMetaAnnotation) {
        T annotation = field.getAnnotation(annotationClass);
        if (annotation != null || !searchMetaAnnotation) {
            return annotation;
        }
        return Arrays.stream(field.getAnnotations())
                .map(a -> a.annotationType().getAnnotation(annotationClass))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Build field path
     */
    private static String buildFieldPath(String parentPath, String fieldName) {
        return parentPath.isEmpty() ? fieldName : parentPath + "." + fieldName;
    }

    /**
     * Process complex field values
     */
    private static <T extends Annotation> void processComplexFieldValue(
            Object fieldValue,
            Class<T> annotationClass,
            List<AnnotatedField<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        if (fieldValue instanceof Collection<?> collection) {
            processCollection(collection, annotationClass, results, visited,
                    currentPath, searchMetaAnnotation);
        } else if (fieldValue.getClass().isArray()) {
            processArray((Object[]) fieldValue, annotationClass, results,
                    visited, currentPath, searchMetaAnnotation);
        } else if (fieldValue instanceof Map<?, ?> map) {
            processMap(map, annotationClass, results, visited, currentPath,
                    searchMetaAnnotation);
        } else {
            collectAnnotatedFields(fieldValue, annotationClass, results,
                    visited, currentPath, searchMetaAnnotation);
        }
    }

    /**
     * Process collection elements
     */
    private static <T extends Annotation> void processCollection(
            Collection<?> collection,
            Class<T> annotationClass,
            List<AnnotatedField<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        int index = 0;
        for (Object item : collection) {
            if (item != null && isComplexObject(item.getClass())) {
                collectAnnotatedFields(item, annotationClass, results, visited,
                        currentPath + "[" + index + "]", searchMetaAnnotation);
            }
            index++;
        }
    }

    /**
     * Process array elements
     */
    private static <T extends Annotation> void processArray(
            Object[] array,
            Class<T> annotationClass,
            List<AnnotatedField<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        for (int i = 0; i < array.length; i++) {
            Object item = array[i];
            if (item != null && isComplexObject(item.getClass())) {
                collectAnnotatedFields(item, annotationClass, results, visited,
                        currentPath + "[" + i + "]", searchMetaAnnotation);
            }
        }
    }

    /**
     * Process map entries
     */
    private static <T extends Annotation> void processMap(
            Map<?, ?> map,
            Class<T> annotationClass,
            List<AnnotatedField<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && isComplexObject(value.getClass())) {
                collectAnnotatedFields(value, annotationClass, results, visited,
                        currentPath + "[" + entry.getKey() + "]",
                        searchMetaAnnotation);
            }
        }
    }

    /**
     * Get all fields of a class
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = clazz;

            while (currentClass != null && currentClass != Object.class) {
                fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                currentClass = currentClass.getSuperclass();
            }

            return fields;
        });
    }

    /**
     * Check if primitive or common type
     */
    private static boolean isPrimitiveOrCommonType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        return PRIMITIVE_TYPES.contains(clazz) ||
                clazz.isPrimitive() ||
                clazz.isEnum() ||
                clazz.getPackageName().startsWith("java.") ||
                clazz.getPackageName().startsWith("javax.");
    }

    /**
     * Check if complex object
     */
    private static boolean isComplexObject(Class<?> clazz) {
        if (isCollection(clazz) || clazz.isArray()) {
            return true;
        }

        if (clazz.isArray()) {
            return !isPrimitiveOrCommonType(clazz.getComponentType());
        }

        return !isPrimitiveOrCommonType(clazz);
    }

    /**
     * Check if class is a collection type
     */
    private static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    /**
     * Check if field should be skipped
     */
    private static boolean isFinalAndStatic(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers);
    }

    private static boolean isGenerics(Class<?> clazz) {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        return typeParameters.length > 0;
    }
}