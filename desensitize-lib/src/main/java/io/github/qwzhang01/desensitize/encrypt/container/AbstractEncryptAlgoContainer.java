package io.github.qwzhang01.desensitize.encrypt.container;

import io.github.qwzhang01.desensitize.encrypt.shield.DefaultEncryptionAlgo;
import io.github.qwzhang01.desensitize.encrypt.shield.EncryptionAlgo;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract container for encryption algorithm management using Factory Pattern.
 *
 * <p>This base class provides a template for encryption algorithm containers with:</p>
 * <ul>
 *   <li>Lazy instantiation with caching to avoid repeated object creation</li>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 *   <li>Spring context integration for dependency injection</li>
 *   <li>Fallback mechanism to default algorithm on failure</li>
 * </ul>
 *
 * @author avinzhang
 */
public abstract class AbstractEncryptAlgoContainer {

    private static final Logger log = LoggerFactory.getLogger(AbstractEncryptAlgoContainer.class);

    /**
     * Cache for algorithm instances to avoid repeated creation.
     * Uses ConcurrentHashMap for thread-safe operations without explicit synchronization.
     */
    private static final ConcurrentHashMap<Class<? extends EncryptionAlgo>, EncryptionAlgo> ALGO_CACHE
            = new ConcurrentHashMap<>();

    /**
     * Clears the algorithm cache.
     * Useful for testing scenarios or when algorithms need to be reloaded at runtime.
     */
    public static void clearCache() {
        log.debug("Clearing encryption algorithm cache");
        ALGO_CACHE.clear();
    }

    /**
     * Gets the default encryption algorithm instance.
     * This is a convenience method that delegates to the implementation's default algorithm.
     *
     * @return the default encryption algorithm instance
     */
    public final EncryptionAlgo getAlgo() {
        EncryptionAlgo defaultAlgo = defaultEncryptAlgo();
        if (defaultAlgo != null) {
            return defaultAlgo;
        }
        log.warn("Default encryption algorithm is null, falling back to DefaultEncryptionAlgo");
        return getAlgo(DefaultEncryptionAlgo.class);
    }

    /**
     * Gets an encryption algorithm instance by class type using Factory Pattern.
     *
     * <p>The algorithm retrieval follows this priority:</p>
     * <ol>
     *   <li>Return cached instance if available</li>
     *   <li>Try to get bean from Spring context (supports dependency injection)</li>
     *   <li>Create new instance via reflection</li>
     *   <li>Fallback to default algorithm on failure</li>
     * </ol>
     *
     * @param clazz the encryption algorithm class
     * @return the encryption algorithm instance
     * @throws DesensitizeException if algorithm cannot be created and no fallback is available
     */
    public final EncryptionAlgo getAlgo(Class<? extends EncryptionAlgo> clazz) {
        if (clazz == null) {
            log.warn("Null algorithm class provided, using default algorithm");
            return getAlgo();
        }

        // If requesting the interface type, return default implementation
        if (EncryptionAlgo.class.equals(clazz)) {
            return defaultEncryptAlgo();
        }

        return ALGO_CACHE.computeIfAbsent(clazz, this::createAlgorithmInstance);
    }

    /**
     * Creates a new algorithm instance with fallback mechanism.
     * This method is called by computeIfAbsent and should not be called directly.
     *
     * @param clazz the algorithm class to instantiate
     * @return the created algorithm instance
     * @throws DesensitizeException if creation fails and no fallback is available
     */
    private EncryptionAlgo createAlgorithmInstance(Class<? extends EncryptionAlgo> clazz) {
        log.debug("Creating new instance of encryption algorithm: {}", clazz.getName());

        // Strategy 1: Try to get from Spring context (supports dependency injection)
        if (SpringContextUtil.isInitialized()) {
            EncryptionAlgo algo = SpringContextUtil.getBeanSafely(clazz);
            if (algo != null) {
                log.debug("Retrieved encryption algorithm from Spring context: {}", clazz.getName());
                return algo;
            }
        }

        // Strategy 2: Try direct instantiation via reflection
        try {
            EncryptionAlgo instance = clazz.getDeclaredConstructor().newInstance();
            log.debug("Successfully created encryption algorithm instance: {}", clazz.getName());
            return instance;
        } catch (Exception e) {
            log.error("Failed to instantiate encryption algorithm: {}", clazz.getName(), e);
            return handleInstantiationFailure(clazz, e);
        }
    }

    /**
     * Handles algorithm instantiation failure with fallback mechanism.
     *
     * @param clazz the algorithm class that failed to instantiate
     * @param cause the exception that caused the failure
     * @return the fallback algorithm instance
     * @throws DesensitizeException if fallback also fails
     */
    private EncryptionAlgo handleInstantiationFailure(Class<? extends EncryptionAlgo> clazz, Exception cause) {
        // If not already trying the default algorithm, fall back to it
        if (!clazz.equals(DefaultEncryptionAlgo.class)) {
            log.warn("Falling back to default encryption algorithm due to instantiation failure");
            try {
                return new DefaultEncryptionAlgo();
            } catch (Exception ex) {
                throw new DesensitizeException(
                        "Failed to create encryption algorithm instance and fallback also failed", ex);
            }
        }

        // If default algorithm itself fails, throw exception
        throw new DesensitizeException(
                "Failed to create default encryption algorithm instance", cause);
    }

    /**
     * Returns the default encryption algorithm for this container.
     * Subclasses must implement this method to provide their specific default algorithm.
     *
     * @return the default encryption algorithm instance
     */
    public abstract EncryptionAlgo defaultEncryptAlgo();

    /**
     * Gets the current size of the algorithm cache.
     * Useful for monitoring and debugging purposes.
     *
     * @return the number of cached algorithm instances
     */
    public final int getCacheSize() {
        return ALGO_CACHE.size();
    }
}
