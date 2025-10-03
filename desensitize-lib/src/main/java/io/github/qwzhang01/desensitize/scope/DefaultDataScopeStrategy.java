package io.github.qwzhang01.desensitize.scope;

/**
 * Default data scope strategy implementation.
 * This is a no-operation strategy that performs no data scope filtering.
 * It can be used as a fallback or when no specific data scope rules are required.
 *
 * @author avinzhang
 */
public class DefaultDataScopeStrategy implements DataScopeStrategy {

    @Override
    public String join() {
        return "";
    }

    @Override
    public String where() {
        return "";
    }
}