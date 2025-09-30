package io.github.qwzhang01.desensitize.annotation;

import io.github.qwzhang01.desensitize.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.shield.IdCardCoverAlgo;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaskId {
    Class<? extends CoverAlgo> value() default IdCardCoverAlgo.class;
}
