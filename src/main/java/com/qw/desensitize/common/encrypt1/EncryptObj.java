package com.qw.desensitize.common.encrypt1;

import java.lang.annotation.*;

/**
 * 敏感信息类注解
 *
 * @author avinzhang
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptObj {
}