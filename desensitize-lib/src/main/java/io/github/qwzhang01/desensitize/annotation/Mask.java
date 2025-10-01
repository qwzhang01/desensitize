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
 * @author avinzhang
 * @see CoverAlgo
 * @see DefaultCoverAlgo
 * @since 1.0.0
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