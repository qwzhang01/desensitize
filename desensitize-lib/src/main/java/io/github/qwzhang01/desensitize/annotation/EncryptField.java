package io.github.qwzhang01.desensitize.annotation;

import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

import java.lang.annotation.*;

/**
 * Annotation for marking fields that require encryption.
 * 
 * <p>This annotation is used to mark entity fields that contain sensitive data
 * and should be encrypted before storage and decrypted when retrieved.
 * The annotation allows specifying a custom encryption algorithm implementation.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * public class User {
 *     {@code @EncryptField}
 *     private String phoneNumber;
 *     
 *     {@code @EncryptField(CustomEncryptionAlgo.class)}
 *     private String socialSecurityNumber;
 * }
 * </pre>
 * 
 * <p>The annotation is processed by interceptors that automatically handle
 * encryption during database operations.</p>
 *
 * @author qwzhang01
 * @since 1.0.0
 * @see EncryptionAlgo
 * @see DefaultEncryptionAlgo
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EncryptField {
    
    /**
     * Specifies the encryption algorithm class to use for this field.
     * If not specified, the default encryption algorithm will be used.
     * 
     * @return the encryption algorithm class
     */
    Class<? extends EncryptionAlgo> value() default DefaultEncryptionAlgo.class;
}