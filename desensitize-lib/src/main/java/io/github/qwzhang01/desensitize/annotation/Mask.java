package io.github.qwzhang01.desensitize.annotation;

import io.github.qwzhang01.desensitize.shield.CoverAlgo;
import io.github.qwzhang01.desensitize.shield.DefaultCoverAlgo;

import java.lang.annotation.*;

/**
 * Annotation for marking fields or types that require data masking.
 * 
 * <p>This annotation can be applied to fields or entire classes to indicate
 * that the data should be masked when displayed or logged. The annotation
 * allows specifying a custom masking algorithm implementation.</p>
 * 
 * <p>When applied to a field, only that specific field will be masked.
 * When applied to a class, all applicable fields in the class will be masked
 * using the specified algorithm.</p>
 * 
 * <p>Usage examples:</p>
 * <pre>
 * // Field-level masking
 * public class User {
 *     {@code @Mask}
 *     private String phoneNumber;
 *     
 *     {@code @Mask(CustomCoverAlgo.class)}
 *     private String email;
 * }
 * 
 * // Class-level masking
 * {@code @Mask(DefaultCoverAlgo.class)}
 * public class SensitiveData {
 *     private String data1;
 *     private String data2;
 * }
 * </pre>
 * 
 * @author qwzhang01
 * @since 1.0.0
 * @see CoverAlgo
 * @see DefaultCoverAlgo
 */
@Inherited
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mask {
    
    /**
     * Specifies the masking algorithm class to use for this field or type.
     * If not specified, the default masking algorithm will be used.
     * 
     * @return the masking algorithm class
     */
    Class<? extends CoverAlgo> value() default DefaultCoverAlgo.class;
}