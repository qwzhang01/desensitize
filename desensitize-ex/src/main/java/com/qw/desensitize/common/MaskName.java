package com.qw.desensitize.common;

import io.github.qwzhang01.desensitize.annotation.Mask;

import java.lang.annotation.*;

/**
 * example
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mask(value = NameCoverAlgo.class)
public @interface MaskName {
}