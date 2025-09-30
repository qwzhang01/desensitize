package io.github.qwzhang01.desensitize.shield;

/**
 * Default data masking algorithm implementation.
 * Extends RoutineCoverAlgo to provide a comprehensive set of masking strategies
 * for various types of sensitive data including phone numbers, ID cards, emails, and names.
 * 
 * <p>This class serves as the primary implementation for data masking operations
 * and can be easily extended or customized for specific business requirements.</p>
 *
 * @author qwzhang01
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