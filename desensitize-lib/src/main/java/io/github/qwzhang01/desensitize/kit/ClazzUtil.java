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
 * Class operation utility.
 * Provides utility methods for class reflection, field analysis, and annotation processing.
 *
 * @author avinzhang
 */
public final class ClazzUtil {
    private final static Set<Class<?>> NO_CLASS = new CopyOnWriteArraySet<>();
    /**
     * Cache for processed classes to avoid repeated reflection operations
     */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    /**
     * Cache for checked types to avoid infinite recursion
     */
    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            String.class, Integer.class, Long.class, Double.class, Float.class,
            Boolean.class, Byte.class, Short.class, Character.class,
            int.class, long.class, double.class, float.class,
            boolean.class, byte.class, short.class, char.class
    );

    /**
     * 获取对象的属性值
     */
    public static Object getPropertyValue(Object obj, String propertyName) throws Exception {
        if (obj == null || propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("对象和属性名不能为空");
        }

        String capitalizedName = StringUtils.capitalize(propertyName);
        
        // 尝试 getter 方法
        Object result = tryGetterMethod(obj, "get" + capitalizedName);
        if (result != null) {
            return result;
        }
        
        // 尝试 boolean 类型的 is 方法
        result = tryGetterMethod(obj, "is" + capitalizedName);
        if (result != null) {
            return result;
        }
        
        // 直接通过字段访问
        return getFieldValue(obj, propertyName);
    }

    /**
     * 设置对象的属性值
     */
    public static void setPropertyValue(Object obj, String propertyName, Object value) throws Exception {
        if (obj == null || propertyName == null || propertyName.trim().isEmpty()) {
            throw new IllegalArgumentException("对象和属性名不能为空");
        }

        String setterName = "set" + StringUtils.capitalize(propertyName);
        
        // 尝试通过 setter 方法设置
        if (trySetterMethod(obj, setterName, value)) {
            return;
        }
        
        // 直接通过字段设置
        setFieldValue(obj, propertyName, value);
    }

    /**
     * 尝试调用 getter 方法
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
     * 尝试调用 setter 方法
     */
    private static boolean trySetterMethod(Object obj, String methodName, Object value) {
        try {
            Method setter = obj.getClass().getMethod(methodName, value != null ? value.getClass() : Object.class);
            setter.invoke(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 通过字段获取值
     */
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new DesensitizeException("无法获取属性: " + fieldName);
        }
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 通过字段设置值
     */
    private static void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new DesensitizeException("无法设置属性: " + fieldName);
        }
        field.setAccessible(true);
        field.set(obj, value);
    }

    /**
     * 在类层次结构中查找方法
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
     * 递归查找字段（包括父类）
     * 在类层次结构中查找字段
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
     * Recursively retrieves all fields with specified annotation from object and its complex properties
     * This method searches for meta-annotations (annotations on annotations)
     *
     * @param obj             the object to inspect
     * @param annotationClass the annotation class to search for
     * @return list of results containing fields and their corresponding objects
     */
    public static <T extends Annotation> List<AnnotatedFieldResult<T>> getAnnotatedFieldsWithMetaAnnotation(
            Object obj, Class<T> annotationClass) {
        return getAnnotatedFieldsInternal(obj, annotationClass, true);
    }

    /**
     * Recursively retrieves all fields with specified annotation from object and its complex properties
     * This method searches for direct annotations only
     *
     * @param obj             the object to inspect
     * @param annotationClass the annotation class to search for
     * @return list of results containing fields and their corresponding objects
     */
    public static <T extends Annotation> List<AnnotatedFieldResult<T>> getAnnotatedFields(
            Object obj, Class<T> annotationClass) {
        return getAnnotatedFieldsInternal(obj, annotationClass, false);
    }

    /**
     * Internal method for retrieving annotated fields
     *
     * @param obj                the object to inspect
     * @param annotationClass    the annotation class to search for
     * @param searchMetaAnnotation whether to search for meta-annotations
     * @return list of results containing fields and their corresponding objects
     */
    private static <T extends Annotation> List<AnnotatedFieldResult<T>> getAnnotatedFieldsInternal(
            Object obj, Class<T> annotationClass, boolean searchMetaAnnotation) {
        if (obj == null || annotationClass == null) {
            return Collections.emptyList();
        }

        if (NO_CLASS.contains(obj.getClass())) {
            return Collections.emptyList();
        }

        List<AnnotatedFieldResult<T>> results = new ArrayList<>();
        Set<Object> visited = new HashSet<>();

        collectAnnotatedFields(obj, annotationClass, results, visited, "", searchMetaAnnotation);

        if (results.isEmpty() && !isCollection(obj.getClass()) && !isGenerics(obj.getClass())) {
            NO_CLASS.add(obj.getClass());
        }

        return results;
    }

    /**
     * Recursively collects fields with specified annotation
     *
     * @param obj                  the current object being inspected
     * @param annotationClass      the annotation class to search for
     * @param results              result collector
     * @param visited              set of visited objects to prevent circular references
     * @param fieldPath            field path for debugging and tracking
     * @param searchMetaAnnotation whether to search for meta-annotations
     */
    private static <T extends Annotation> void collectAnnotatedFields(
            Object obj,
            Class<T> annotationClass,
            List<AnnotatedFieldResult<T>> results,
            Set<Object> visited,
            String fieldPath,
            boolean searchMetaAnnotation) {

        if (obj == null || visited.contains(obj)) {
            return;
        }

        Class<?> objClass = obj.getClass();

        // Skip primitive types and common Java types
        if (isPrimitiveOrCommonType(objClass)) {
            return;
        }

        visited.add(obj);

        try {
            // Get all fields from current class and its parent classes
            List<Field> fields = getAllFields(objClass);

            for (Field field : fields) {
                if (isFinalAndStatic(field)) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    // Check if field has target annotation
                    T annotation = findAnnotation(field, annotationClass, searchMetaAnnotation);
                    if (annotation != null) {
                        String currentPath = buildFieldPath(fieldPath, field.getName());
                        results.add(new AnnotatedFieldResult<>(field, obj, annotation, currentPath));
                    }

                    // Process field value recursively
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null && isComplexObject(fieldValue.getClass())) {
                        String currentPath = buildFieldPath(fieldPath, field.getName());
                        processComplexFieldValue(fieldValue, annotationClass, results, visited, currentPath, searchMetaAnnotation);
                    }
                } catch (IllegalAccessException e) {
                    // Ignore inaccessible fields
                }
            }
        } finally {
            visited.remove(obj);
        }
    }

    /**
     * Find annotation on field, with optional meta-annotation search
     */
    private static <T extends Annotation> T findAnnotation(Field field, Class<T> annotationClass, boolean searchMetaAnnotation) {
        // First try direct annotation
        T annotation = field.getAnnotation(annotationClass);
        if (annotation != null || !searchMetaAnnotation) {
            return annotation;
        }

        // Search for meta-annotations
        return Arrays.stream(field.getAnnotations())
                .map(a -> a.annotationType().getAnnotation(annotationClass))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Build field path string
     */
    private static String buildFieldPath(String parentPath, String fieldName) {
        return parentPath.isEmpty() ? fieldName : parentPath + "." + fieldName;
    }

    /**
     * Process complex field values (collections, arrays, maps, objects)
     */
    private static <T extends Annotation> void processComplexFieldValue(
            Object fieldValue,
            Class<T> annotationClass,
            List<AnnotatedFieldResult<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        // Handle collection types
        if (fieldValue instanceof Collection<?> collection) {
            processCollection(collection, annotationClass, results, visited, currentPath, searchMetaAnnotation);
        }
        // Handle array types
        else if (fieldValue.getClass().isArray()) {
            processArray((Object[]) fieldValue, annotationClass, results, visited, currentPath, searchMetaAnnotation);
        }
        // Handle Map types
        else if (fieldValue instanceof Map<?, ?> map) {
            processMap(map, annotationClass, results, visited, currentPath, searchMetaAnnotation);
        }
        // Handle regular complex objects
        else {
            collectAnnotatedFields(fieldValue, annotationClass, results, visited, currentPath, searchMetaAnnotation);
        }
    }

    /**
     * Process collection elements
     */
    private static <T extends Annotation> void processCollection(
            Collection<?> collection,
            Class<T> annotationClass,
            List<AnnotatedFieldResult<T>> results,
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
            List<AnnotatedFieldResult<T>> results,
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
            List<AnnotatedFieldResult<T>> results,
            Set<Object> visited,
            String currentPath,
            boolean searchMetaAnnotation) {

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value != null && isComplexObject(value.getClass())) {
                collectAnnotatedFields(value, annotationClass, results, visited,
                        currentPath + "[" + entry.getKey() + "]", searchMetaAnnotation);
            }
        }
    }

    /**
     * Get all fields of a class (including parent classes)
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
     * Determine if it's a primitive type or common type
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
     * Determine if it's a complex object (object that needs recursive processing)
     */
    private static boolean isComplexObject(Class<?> clazz) {
        // Collections, arrays, and maps are always complex
        if (isCollection(clazz) || clazz.isArray()) {
            return true;
        }
        
        // For arrays, check component type
        if (clazz.isArray()) {
            return !isPrimitiveOrCommonType(clazz.getComponentType());
        }
        
        // Other types are complex if they're not primitive/common types
        return !isPrimitiveOrCommonType(clazz);
    }

    /**
     * Check if class is a collection type
     */
    private static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz);
    }

    /**
     * Check if field should be skipped (static, final, or transient)
     */
    private static boolean isFinalAndStatic(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers);
    }

    private static boolean isGenerics(Class<?> clazz) {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        return typeParameters.length > 0;
    }

    /**
     * Annotated field result record containing field, containing object, annotation, and field path
     *
     * @param field            the annotated field
     * @param containingObject the object containing this field
     * @param annotation       the annotation instance
     * @param fieldPath        the field path for debugging and tracking
     */
    public record AnnotatedFieldResult<T extends Annotation>(
            Field field, 
            Object containingObject, 
            T annotation,
            String fieldPath) {

        /**
         * Get the field value safely
         */
        public Object getFieldValue() {
            try {
                field.setAccessible(true);
                return field.get(containingObject);
            } catch (IllegalAccessException e) {
                throw new DesensitizeException("Cannot access field value: " + field.getName(), e);
            }
        }

        /**
         * Set the field value safely
         */
        public void setFieldValue(Object value) {
            try {
                field.setAccessible(true);
                field.set(containingObject, value);
            } catch (IllegalAccessException e) {
                throw new DesensitizeException("Cannot set field value: " + field.getName(), e);
            }
        }

        @Override
        public String toString() {
            return String.format("AnnotatedFieldResult{field=%s, path=%s, annotation=%s}",
                    field.getName(), fieldPath, annotation.annotationType().getSimpleName());
        }
    }
}