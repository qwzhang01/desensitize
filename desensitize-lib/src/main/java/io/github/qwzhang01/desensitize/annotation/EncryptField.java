/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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
 * @author avinzhang
 * @see EncryptionAlgo
 * @see DefaultEncryptionAlgo
 * @since 1.0.0
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