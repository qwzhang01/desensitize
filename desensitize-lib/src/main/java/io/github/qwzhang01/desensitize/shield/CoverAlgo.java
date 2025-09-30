package io.github.qwzhang01.desensitize.shield;

/**
 * Data masking algorithm interface.
 * Provides contract for implementing various data masking strategies
 * to protect sensitive information.
 *
 * @author qwzhang01
 * @since 1.0.0
 */
public interface CoverAlgo {

    /**
     * Masks sensitive data content according to the specific algorithm implementation.
     * This is the main entry point for data masking operations.
     *
     * @param content the sensitive data content to be masked, can be null or empty
     * @return the masked data content, or original content if masking is not applicable
     */
    String mask(String content);
}
