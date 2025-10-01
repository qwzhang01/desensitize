package io.github.qwzhang01.desensitize.scope;

import java.util.Collections;
import java.util.List;

/**
 * Default data scope strategy implementation.
 * This is a no-operation strategy that performs no data scope filtering.
 * It can be used as a fallback or when no specific data scope rules are required.
 *
 * @author avinzhang
 */
public class DefaultDataScopeStrategy extends DataScopeStrategy {
    @Override
    protected void configJoin() {
        //do nothing
    }

    @Override
    protected List<Where> configWhere() {
        //do nothing
        return Collections.emptyList();
    }
}
