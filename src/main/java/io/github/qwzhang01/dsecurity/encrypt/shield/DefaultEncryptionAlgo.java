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


package io.github.qwzhang01.dsecurity.encrypt.shield;

import io.github.qwzhang01.dsecurity.exception.DesensitizeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.util.Base64;

/**
 * Default encryption algorithm implementation using DES (Data Encryption
 * Standard).
 * Provides symmetric encryption and decryption capabilities for sensitive
 * data protection.
 *
 * <p>This implementation uses DES algorithm with CBC mode and PKCS5 padding
 * for secure data transformation. The encryption key and initialization vector
 * are predefined for consistency across the application.</p>
 *
 * <p><strong>Security Note:</strong> DES is considered weak by modern
 * standards.
 * For production use, consider upgrading to AES or other stronger algorithms
 * .</p>
 *
 * @author avinzhang
 * @see EncryptionAlgo
 * @since 1.0.0
 */
public class DefaultEncryptionAlgo implements EncryptionAlgo {
    private static final String ENCRYPT_PREFIX = "_sensitive_start_";
    private static final Logger log =
            LoggerFactory.getLogger(DefaultEncryptionAlgo.class);

    /**
     * Encrypts the given value using the default DES encryption algorithm.
     *
     * @param value the plain text value to encrypt
     * @return the encrypted value encoded in Base64, or null if input is null
     */
    @Override
    public String encrypt(String value) {
        if (!value.startsWith(ENCRYPT_PREFIX)) {
            String encrypted = DesKit.encrypt(DesKit.KEY, value);
            return ENCRYPT_PREFIX + encrypted;
        }
        return value;
    }

    /**
     * Decrypts the given encrypted value using the default DES decryption
     * algorithm.
     *
     * @param value the encrypted value (Base64 encoded) to decrypt
     * @return the decrypted plain text value, or null if input is null
     */
    @Override
    public String decrypt(String value) {
        if (value.startsWith(ENCRYPT_PREFIX)) {
            value = value.substring(17);
        }
        return DesKit.decrypt(DesKit.KEY, value);
    }

    /**
     * Internal utility class for DES encryption and decryption operations.
     * Encapsulates all cryptographic operations and configuration constants.
     *
     * @author avinzhang
     */
    private static class DesKit {

        /**
         * Default encryption key (must be at least 8 characters for DES).
         * In production environments, this should be externalized and secured.
         */
        public static final String KEY = "key12345678";

        /**
         * Initialization Vector parameter for CBC mode (must be exactly 8
         * bytes for DES).
         * Provides additional security by ensuring identical plaintexts
         * produce different ciphertexts.
         */
        private static final String IV_PARAMETER = "12345678";

        /**
         * The DES encryption algorithm identifier.
         */
        private static final String ALGORITHM = "DES";

        /**
         * Complete cipher transformation string specifying algorithm, mode,
         * and padding.
         * DES/CBC/PKCS5Padding provides secure encryption with proper padding.
         */
        private static final String CIPHER_ALGORITHM = "DES/CBC/PKCS5Padding";

        /**
         * Character encoding used for string-to-byte conversions.
         */
        private static final String CHARSET = "utf-8";

        /**
         * Generates a DES secret key from the provided password string.
         * The password is converted to bytes and used to create a DES key
         * specification.
         *
         * @param password the password string to generate key from (must be
         *                 at least 8 characters)
         * @return the generated DES secret key
         * @throws Exception if key generation fails due to invalid password
         *                   or algorithm issues
         */
        private static Key generateKey(String password) throws Exception {
            DESKeySpec dks = new DESKeySpec(password.getBytes(CHARSET));
            SecretKeyFactory keyFactory =
                    SecretKeyFactory.getInstance(ALGORITHM);
            return keyFactory.generateSecret(dks);
        }

        /**
         * Encrypts a string using DES algorithm with CBC mode and PKCS5
         * padding.
         * The encrypted result is encoded in Base64 for safe string
         * representation.
         *
         * @param password the encryption password (must be at least 8
         *                 characters long)
         * @param data     the plain text data to encrypt
         * @return the encrypted data encoded in Base64, or null if input
         * data is null
         * @throws RuntimeException if password is invalid (null or less than
         *                          8 characters)
         */
        public static String encrypt(String password, String data) {
            if (password == null || password.length() < 8) {
                throw new DesensitizeException("Encryption failed: key must " +
                        "be at least 8 characters long");
            }
            if (data == null) {
                return null;
            }

            try {
                Key secretKey = generateKey(password);
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                IvParameterSpec iv =
                        new IvParameterSpec(IV_PARAMETER.getBytes(CHARSET));
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

                // Perform encryption
                byte[] encryptedBytes = cipher.doFinal(data.getBytes(CHARSET));

                // Encode to Base64 for safe string representation
                byte[] encodedBytes =
                        Base64.getEncoder().encode(encryptedBytes);

                return new String(encodedBytes);

            } catch (Exception e) {
                log.error("encrypt error", e);
                return data; // Return original data if encryption fails
            }
        }

        /**
         * Decrypts a Base64-encoded encrypted string using DES algorithm.
         * The input should be a Base64-encoded string produced by the
         * encrypt method.
         *
         * @param password the decryption password (must be at least 8
         *                 characters long)
         * @param data     the encrypted data (Base64 encoded) to decrypt
         * @return the decrypted plain text, or null if input data is null
         * @throws RuntimeException if password is invalid (null or less than
         *                          8 characters)
         */
        public static String decrypt(String password, String data) {
            if (password == null || password.length() < 8) {
                throw new DesensitizeException("Decryption failed: key must " +
                        "be at least 8 characters long");
            }
            if (data == null) {
                return null;
            }

            try {
                Key secretKey = generateKey(password);
                Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
                IvParameterSpec iv =
                        new IvParameterSpec(IV_PARAMETER.getBytes(CHARSET));
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

                // Decode from Base64
                byte[] decodedBytes =
                        Base64.getDecoder().decode(data.getBytes(CHARSET));

                // Perform decryption
                byte[] decryptedBytes = cipher.doFinal(decodedBytes);

                return new String(decryptedBytes, CHARSET);
            } catch (Exception e) {
                log.error("decrypt error", e);
                return data; // Return original data if decryption fails
            }
        }
    }
}