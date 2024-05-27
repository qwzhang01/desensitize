package com.qw.desensitize.common.encrypt1;

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
}

