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


package io.github.qwzhang01.desensitize.mask.annotation;

import io.github.qwzhang01.desensitize.mask.shield.EmailCoverAlgo;

import java.lang.annotation.*;

/**
 * Annotation for marking email address fields that require masking.
 *
 * <p>This annotation specifically targets email address fields and applies
 * email masking algorithm. The email masking typically keeps the first and
 * last characters of the username while masking the middle part, leaving
 * the domain unchanged.</p>
 *
 * <p>This annotation is a specialized version of {@link Mask} annotation
 * with EmailCoverAlgo as the default masking algorithm.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * public class User {
 *     {@code @MaskEmail}
 *     private String email; // Will be masked as u***r@example.com
 * }
 * </pre>
 *
 * @author avinzhang
 * @see Mask
 * @see EmailCoverAlgo
 * @since 1.0.0
 */
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mask(value = EmailCoverAlgo.class)
public @interface MaskEmail {
    // This annotation uses the @Mask meta-annotation with EmailCoverAlgo
    // No additional attributes are needed as the behavior is defined by the meta-annotation
}
