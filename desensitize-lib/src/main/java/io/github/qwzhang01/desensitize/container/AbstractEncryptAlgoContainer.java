package io.github.qwzhang01.desensitize.container;

import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密解密算法容器基类
 *
 * @author avinzhang
 */
public abstract class AbstractEncryptAlgoContainer {
    /**
     * Cache for algorithm instances to avoid repeated creation
     */
    private static final ConcurrentHashMap<Class<? extends EncryptionAlgo>, EncryptionAlgo> ALGO_CACHE = new ConcurrentHashMap<>();

    /**
     * Clears the algorithm cache. Useful for testing or when algorithms need to be reloaded.
     */
    public static void clearCache() {
        ALGO_CACHE.clear();
    }

    /**
     * Gets the default encryption algorithm instance.
     *
     * @return the default encryption algorithm instance
     */
    public EncryptionAlgo getAlgo() {
        if (defaultEncryptAlgo() != null) {
            return defaultEncryptAlgo();
        }
        return getAlgo(DefaultEncryptionAlgo.class);
    }

    /**
     * Gets an encryption algorithm instance by class type.
     * First tries to get from Spring context, then falls back to direct instantiation.
     *
     * @param clazz the encryption algorithm class
     * @return the encryption algorithm instance
     */
    public EncryptionAlgo getAlgo(Class<? extends EncryptionAlgo> clazz) {
        if (EncryptionAlgo.class.equals(clazz)) {
            return defaultEncryptAlgo();
        }
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
                        throw new DesensitizeException("Failed to create encryption algorithm instance", ex);
                    }
                }
                throw new DesensitizeException("Failed to create encryption algorithm instance", e);
            }
        });
    }

    public abstract EncryptionAlgo defaultEncryptAlgo();
}
