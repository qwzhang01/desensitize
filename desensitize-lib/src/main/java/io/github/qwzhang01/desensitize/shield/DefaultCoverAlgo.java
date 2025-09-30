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

package io.github.qwzhang01.desensitize.shield;

/**
 * Default data masking algorithm implementation.
 * Extends RoutineCoverAlgo to provide a comprehensive set of masking strategies
 * for various types of sensitive data including phone numbers, ID cards, emails, and names.
 * 
 * <p>This class serves as the primary implementation for data masking operations
 * and can be easily extended or customized for specific business requirements.</p>
 *
 * @author avinzhang
 * @since 1.0.0
 * @see RoutineCoverAlgo
 * @see CoverAlgo
 */
public class DefaultCoverAlgo extends RoutineCoverAlgo {
    
    /**
     * Default constructor.
     * Creates a new instance with all inherited masking capabilities from RoutineCoverAlgo.
     */
    public DefaultCoverAlgo() {
        super();
    }
}