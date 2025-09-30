package io.github.qwzhang01.desensitize.annotation;

import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

import java.lang.annotation.*;

/**
 * 敏感字段注解
 *
 * @author avinzhang
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptField {
    Class<? extends EncryptionAlgo> value() default DefaultEncryptionAlgo.class;
}