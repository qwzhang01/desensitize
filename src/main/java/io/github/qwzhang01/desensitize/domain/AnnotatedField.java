package io.github.qwzhang01.desensitize.domain;

import io.github.qwzhang01.desensitize.exception.DesensitizeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Annotated field result record containing field, containing object,
 * annotation, and field path
 *
 * @param field      the annotated field
 * @param obj        the object containing this field
 * @param annotation the annotation instance
 * @param fieldPath  the field path for debugging and tracking
 */
public record AnnotatedField<T extends Annotation>(
        Field field,
        Object obj,
        T annotation,
        String fieldPath) {

    /**
     * Get the field value safely
     */
    public Object getFieldValue() {
        try {
            field.setAccessible(true);
            return field.get(obj);
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
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new DesensitizeException("Cannot set field value: " + field.getName(), e);
        }
    }

    @Override
    public String toString() {
        return String.format("AnnotatedField{field=%s, path=%s, annotation=%s}",
                field.getName(), fieldPath,
                annotation.annotationType().getSimpleName());
    }
}