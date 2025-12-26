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
 * The above copyright notice and this permission notice shall be included in
 *  all
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


package io.github.qwzhang01.desensitize.encrypt.container;

import io.github.qwzhang01.desensitize.encrypt.shield.EncryptionAlgo;

/**
 * Concrete implementation of encryption algorithm container.
 *
 * <p>This container manages encryption algorithm instances using the Factory
 * Pattern
 * combined with Strategy Pattern. It provides:</p>
 * <ul>
 *   <li>Dependency injection support for the default algorithm</li>
 *   <li>Thread-safe lazy instantiation with caching</li>
 *   <li>Automatic fallback mechanism on instantiation failures</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. The default
 * algorithm
 * is immutable after construction, and the cache operations are handled by
 * the parent
 * class using ConcurrentHashMap.</p>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public final class EncryptionAlgoContainer extends AbstractEncryptAlgoContainer {

    /**
     * The default encryption algorithm instance.
     * This field is effectively immutable after construction.
     */
    private final EncryptionAlgo defaultEncryptionAlgo;

    /**
     * Constructs an EncryptionAlgoContainer with a specified default algorithm.
     *
     * <p>The default algorithm should be a fully configured, ready-to-use
     * instance
     * that will be returned when no specific algorithm is requested or when
     * instantiation of other algorithms fails.</p>
     *
     * @param defaultEncryptionAlgo the default encryption algorithm to use
     * @throws IllegalArgumentException if defaultEncryptionAlgo is null
     */
    public EncryptionAlgoContainer(EncryptionAlgo defaultEncryptionAlgo) {
        if (defaultEncryptionAlgo == null) {
            throw new IllegalArgumentException("Default encryption algorithm " +
                    "cannot be null");
        }
        this.defaultEncryptionAlgo = defaultEncryptionAlgo;
    }

    /**
     * Returns the default encryption algorithm for this container.
     * This implementation returns the algorithm instance provided during
     * construction.
     *
     * @return the default encryption algorithm instance, never null
     */
    @Override
    public EncryptionAlgo defaultEncryptAlgo() {
        return defaultEncryptionAlgo;
    }
}