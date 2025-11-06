package io.github.qwzhang01.desensitize.scope;

import java.util.List;

/**
 * Default data scope strategy implementation.
 * This is a no-operation strategy that performs no data scope filtering.
 * It can be used as a fallback or when no specific data scope rules are required.
 *
 * @author avinzhang
 */
public class EmptyDataScopeStrategy<T> implements DataScopeStrategy<T> {

    @Override
    public String join() {
        return "";
    }

    @Override
    public String where() {
        return "";
    }

    @Override
    public void validDs(List<T> validRights) {
        // No-op
    }

    @Override
    public void validDs(List<T> validRights, List<T> withoutRights) {
        // No-op
    }
}