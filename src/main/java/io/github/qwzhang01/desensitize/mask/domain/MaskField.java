package io.github.qwzhang01.desensitize.mask.domain;

import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.mask.annotation.Mask;

import java.lang.reflect.Field;

/**
 * Annotated field result record containing field, containing object, annotation, and field path
 *
 * @param field the annotated field
 * @param obj   the object containing this field
 */
public record MaskField(
        Field field,
        Object obj,
        boolean behest,
        boolean maskFlag,
        Mask annotation) {

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
}