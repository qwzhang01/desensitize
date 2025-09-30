package io.github.qwzhang01.desensitize.shield;

/**
 * 加密算法
 */
public interface EncryptionAlgo {
    String encrypt(String value);

    String decrypt(String value);
}