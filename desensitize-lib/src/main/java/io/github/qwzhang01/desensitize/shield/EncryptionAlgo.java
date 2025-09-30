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
 * @author qwzhang01
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