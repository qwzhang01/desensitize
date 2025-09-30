package io.github.qwzhang01.desensitize.annotation;

import io.github.qwzhang01.desensitize.shield.EmailCoverAlgo;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Mask(value = EmailCoverAlgo.class)
public @interface MaskEmail {
}
