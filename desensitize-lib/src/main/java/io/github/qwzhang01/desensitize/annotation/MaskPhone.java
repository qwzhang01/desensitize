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
import io.github.qwzhang01.desensitize.shield.PhoneCoverAlgo;

import java.lang.annotation.*;

/**
 * Annotation for marking phone number fields that require masking.
 *
 * <p>This annotation specifically targets phone number fields and applies
 * phone number masking algorithm by default. The phone number masking
 * typically keeps the first 3 and last 4 digits while masking the middle digits.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * public class User {
 *     {@code @MaskPhone}
 *     private String phoneNumber; // Will be masked as 138****5678
 * }
 * </pre>
 *
 * @author avinzhang
 * @see CoverAlgo
 * @see PhoneCoverAlgo
 * @since 1.0.0
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaskPhone {

    /**
     * Specifies the masking algorithm class to use for phone number masking.
     * Defaults to PhoneCoverAlgo which implements standard phone number masking.
     *
     * @return the masking algorithm class for phone numbers
     */
    Class<? extends CoverAlgo> value() default PhoneCoverAlgo.class;
}
