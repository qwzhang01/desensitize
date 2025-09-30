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
 * Encryption algorithm interface.
 * Defines the contract for implementing encryption and decryption operations
 * to protect sensitive data through cryptographic transformations.
 * 
 * <p>Implementations of this interface should provide secure, reversible
 * encryption mechanisms suitable for protecting sensitive data in storage
 * and transmission scenarios.</p>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface EncryptionAlgo {

    /**
     * Encrypts the given plain text value using the implemented encryption algorithm.
     * 
     * @param value the plain text value to be encrypted, can be null or empty
     * @return the encrypted value as a string, or null if input is null
     * @throws RuntimeException if encryption fails due to algorithm or configuration issues
     */
    String encrypt(String value);

    /**
     * Decrypts the given encrypted value back to its original plain text form.
     * 
     * @param value the encrypted value to be decrypted, can be null or empty
     * @return the decrypted plain text value, or null if input is null
     * @throws RuntimeException if decryption fails due to invalid input or algorithm issues
     */
    String decrypt(String value);
}