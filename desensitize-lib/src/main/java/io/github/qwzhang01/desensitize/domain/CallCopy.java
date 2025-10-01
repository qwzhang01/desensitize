package io.github.qwzhang01.desensitize.domain;

/**
 * Parameterized method interface for object copying operations.
 * Provides a callback mechanism for custom field transformation logic
 * after the normal copying process is completed.
 *
 * @author avinzhang
 */
@FunctionalInterface
public interface CallCopy<S, T> {
    /**
     * Custom field transformation logic executed after normal copying is completed.
     *
     * @param source the source data object
     * @param target the target data object
     */
    void call(S source, T target);
}
