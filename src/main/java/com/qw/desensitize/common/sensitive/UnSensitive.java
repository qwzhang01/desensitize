package com.qw.desensitize.common.sensitive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏注解
 *
 * @author avinzhang
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UnSensitive {
    /**
     * 标注不同的脱敏算法，比如邮箱脱敏算法、身份证号码脱敏算法、手机号码脱敏算法
     * @return
     */
    String type();
}
