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


package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Container for managing encryption algorithm instances.
 * Provides lazy initialization and caching to avoid circular dependency issues.
 * 
 * @author avinzhang
 * @since 1.0.0
 */
public class EncryptionAlgoContainer {
    
    /**
     * Cache for algorithm instances to avoid repeated creation
     */
    private static final ConcurrentHashMap<Class<? extends EncryptionAlgo>, EncryptionAlgo> ALGO_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Gets the default encryption algorithm instance.
     * 
     * @return the default encryption algorithm instance
     */
    public static EncryptionAlgo getAlgo() {
        return getAlgo(DefaultEncryptionAlgo.class);
    }

    /**
     * Gets an encryption algorithm instance by class type.
     * First tries to get from Spring context, then falls back to direct instantiation.
     * 
     * @param clazz the encryption algorithm class
     * @return the encryption algorithm instance
     */
    public static EncryptionAlgo getAlgo(Class<? extends EncryptionAlgo> clazz) {
        return ALGO_CACHE.computeIfAbsent(clazz, key -> {
            // First try to get from Spring context if available
            if (SpringContextUtil.isInitialized()) {
                EncryptionAlgo algo = SpringContextUtil.getBeanSafely(key);
                if (algo != null) {
                    return algo;
                }
            }
            
            // Fallback to direct instantiation
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // If the requested class fails, try default algorithm
                if (!key.equals(DefaultEncryptionAlgo.class)) {
                    try {
                        return new DefaultEncryptionAlgo();
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to create encryption algorithm instance", ex);
                    }
                }
                throw new RuntimeException("Failed to create encryption algorithm instance", e);
            }
        });
    }
    
    /**
     * Clears the algorithm cache. Useful for testing or when algorithms need to be reloaded.
     */
    public static void clearCache() {
        ALGO_CACHE.clear();
    }
}