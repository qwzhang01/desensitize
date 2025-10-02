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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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
public class ClazzUtil {
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

    public static boolean isWrapper(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        String packageName = clazz.getPackageName();
        return !packageName.startsWith("java.lang")
                && !packageName.startsWith("java.math")
                && !packageName.startsWith("java.time");
    }

    public static <T extends Annotation> List<AnnotatedFieldResult<T>> getAnnotatedAnnotationFields(
            Object obj, Class<T> annotationClass) {

        if (obj == null || annotationClass == null) {
            return Collections.emptyList();
        }

        if (NO_CLASS.contains(obj.getClass())) {
            return Collections.emptyList();
        }

        List<AnnotatedFieldResult<T>> results = new ArrayList<>();
        Set<Object> visited = new HashSet<>();

        collectAnnotatedAnnotationFields(obj, annotationClass, results, visited, "");

        if (results.isEmpty() && !isCollection(obj.getClass()) && !isGenerics(obj.getClass())) {
            NO_CLASS.add(obj.getClass());
            return Collections.emptyList();
        }

        return results;
    }

    /**
     * Recursively retrieves all fields with specified annotation from object and its complex properties
     *
     * @param obj             the object to inspect
     * @param annotationClass the annotation class to search for
     * @return list of results containing fields and their corresponding objects
     */
    public static <T extends Annotation> List<AnnotatedFieldResult<T>> getAnnotatedFields(
            Object obj, Class<T> annotationClass) {
        if (obj == null || annotationClass == null) {
            return Collections.emptyList();
        }

        if (NO_CLASS.contains(obj.getClass())) {
            return Collections.emptyList();
        }

        List<AnnotatedFieldResult<T>> results = new ArrayList<>();
        Set<Object> visited = new HashSet<>();

        collectAnnotatedFields(obj, annotationClass, results, visited, "");

        if (results.isEmpty()) {
            NO_CLASS.add(obj.getClass());
            return Collections.emptyList();
        }

        return results;
    }

    /**
     * Recursively collects fields with specified annotation
     *
     * @param obj             the current object being inspected
     * @param annotationClass the annotation class to search for
     * @param results         result collector
     * @param visited         set of visited objects to prevent circular references
     * @param fieldPath       field path for debugging and tracking
     */
    private static <T extends Annotation> void collectAnnotatedAnnotationFields(
            Object obj,
            Class<T> annotationClass,
            List<AnnotatedFieldResult<T>> results,
            Set<Object> visited,
            String fieldPath) {

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
                    // static final 字段不处理
                    continue;
                }

                field.setAccessible(true);

                try {
                    // Check if field has target annotation
                    T annotation = field.getAnnotation(annotationClass);
                    if (annotation == null) {
                        Annotation[] annotations = field.getAnnotations();
                        if (annotations != null && annotations.length > 0) {
                            annotation = Arrays.stream(annotations).map(a -> {
                                Class<? extends Annotation> annoClazz = a.annotationType();
                                return annoClazz.getAnnotation(annotationClass);
                            }).filter(Objects::nonNull).findAny().orElse(null);
                        }
                    }

                    if (annotation != null) {
                        String currentPath = fieldPath.isEmpty() ? field.getName() : fieldPath + "." + field.getName();
                        results.add(new AnnotatedFieldResult<>(field, obj, annotation, currentPath));
                    }

                    // Get field value
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null) {
                        Class<?> fieldType = fieldValue.getClass();

                        // If it's a complex object, process recursively
                        if (isComplexObject(fieldType)) {
                            String currentPath = fieldPath.isEmpty() ? field.getName() : fieldPath + "." + field.getName();

                            // Handle collection types
                            if (fieldValue instanceof Collection<?> collection) {
                                int index = 0;
                                for (Object item : collection) {
                                    if (item != null && isComplexObject(item.getClass())) {
                                        collectAnnotatedAnnotationFields(item, annotationClass, results, visited,
                                                currentPath + "[" + index + "]");
                                    }
                                    index++;
                                }
                            }
                            // Handle array types
                            else if (fieldType.isArray()) {
                                Object[] array = (Object[]) fieldValue;
                                for (int i = 0; i < array.length; i++) {
                                    Object item = array[i];
                                    if (item != null && isComplexObject(item.getClass())) {
                                        collectAnnotatedAnnotationFields(item, annotationClass, results, visited,
                                                currentPath + "[" + i + "]");
                                    }
                                }
                            }
                            // Handle Map types
                            else if (fieldValue instanceof Map<?, ?> map) {
                                for (Map.Entry<?, ?> entry : map.entrySet()) {
                                    Object value = entry.getValue();
                                    if (value != null && isComplexObject(value.getClass())) {
                                        collectAnnotatedAnnotationFields(value, annotationClass, results, visited,
                                                currentPath + "[" + entry.getKey() + "]");
                                    }
                                }
                            }
                            // Handle regular complex objects
                            else {
                                collectAnnotatedAnnotationFields(fieldValue, annotationClass, results, visited, currentPath);
                            }
                        }
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
     * Recursively collects fields with specified annotation
     *
     * @param obj             the current object being inspected
     * @param annotationClass the annotation class to search for
     * @param results         result collector
     * @param visited         set of visited objects to prevent circular references
     * @param fieldPath       field path for debugging and tracking
     */
    private static <T extends Annotation> void collectAnnotatedFields(
            Object obj,
            Class<T> annotationClass,
            List<AnnotatedFieldResult<T>> results,
            Set<Object> visited,
            String fieldPath) {

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
                    // static final 字段不处理
                    continue;
                }

                field.setAccessible(true);

                try {
                    // Check if field has target annotation
                    T annotation = field.getAnnotation(annotationClass);
                    if (annotation != null) {
                        String currentPath = fieldPath.isEmpty() ? field.getName() : fieldPath + "." + field.getName();
                        results.add(new AnnotatedFieldResult<>(field, obj, annotation, currentPath));
                    }

                    // Get field value
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null) {
                        Class<?> fieldType = fieldValue.getClass();

                        // If it's a complex object, process recursively
                        if (isComplexObject(fieldType)) {
                            String currentPath = fieldPath.isEmpty() ? field.getName() : fieldPath + "." + field.getName();

                            // Handle collection types
                            if (fieldValue instanceof Collection<?> collection) {
                                int index = 0;
                                for (Object item : collection) {
                                    if (item != null && isComplexObject(item.getClass())) {
                                        collectAnnotatedFields(item, annotationClass, results, visited,
                                                currentPath + "[" + index + "]");
                                    }
                                    index++;
                                }
                            }
                            // Handle array types
                            else if (fieldType.isArray()) {
                                Object[] array = (Object[]) fieldValue;
                                for (int i = 0; i < array.length; i++) {
                                    Object item = array[i];
                                    if (item != null && isComplexObject(item.getClass())) {
                                        collectAnnotatedFields(item, annotationClass, results, visited,
                                                currentPath + "[" + i + "]");
                                    }
                                }
                            }
                            // Handle Map types
                            else if (fieldValue instanceof Map<?, ?> map) {
                                for (Map.Entry<?, ?> entry : map.entrySet()) {
                                    Object value = entry.getValue();
                                    if (value != null && isComplexObject(value.getClass())) {
                                        collectAnnotatedFields(value, annotationClass, results, visited,
                                                currentPath + "[" + entry.getKey() + "]");
                                    }
                                }
                            }
                            // Handle regular complex objects
                            else {
                                collectAnnotatedFields(fieldValue, annotationClass, results, visited, currentPath);
                            }
                        }
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
        if (clazz.isArray() || isCollection(clazz)) {
            return true;
        }
        boolean b1 = !isPrimitiveOrCommonType(clazz);
        boolean b2 = !clazz.isArray();
        boolean b3 = clazz.isArray() && !isPrimitiveOrCommonType(clazz.getComponentType());
        return b1 && b2 || b3;
    }

    private static boolean isCollection(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz) ||
                Map.class.isAssignableFrom(clazz) ||
                Collection.class.isAssignableFrom(clazz);
    }

    private static boolean isFinalAndStatic(Field field) {
        return Modifier.isStatic(field.getModifiers())
                || Modifier.isFinal(field.getModifiers())
                || Modifier.isTransient(field.getModifiers());
    }

    private static boolean isGenerics(Class<?> clazz) {
        TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
        return typeParameters.length > 0;
    }

    /**
     * Annotated field result class
     */
    public record AnnotatedFieldResult<T extends Annotation>(Field field, Object containingObject, T annotation,
                                                             String fieldPath) {

        /**
         * Get the field
         */
        @Override
        public Field field() {
            return field;
        }

        /**
         * Get the object containing this field
         */
        @Override
        public Object containingObject() {
            return containingObject;
        }

        /**
         * Get the annotation instance
         */
        @Override
        public T annotation() {
            return annotation;
        }

        /**
         * Get the field path (for debugging)
         */
        @Override
        public String fieldPath() {
            return fieldPath;
        }

        /**
         * Get the field value
         */
        public Object getFieldValue() {
            try {
                field.setAccessible(true);
                return field.get(containingObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field value: " + field.getName(), e);
            }
        }

        /**
         * Set the field value
         */
        public void setFieldValue(Object value) {
            try {
                field.setAccessible(true);
                field.set(containingObject, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot set field value: " + field.getName(), e);
            }
        }

        @Override
        public String toString() {
            return String.format("AnnotatedFieldResult{field=%s, path=%s, annotation=%s}",
                    field.getName(), fieldPath, annotation.annotationType().getSimpleName());
        }
    }
}