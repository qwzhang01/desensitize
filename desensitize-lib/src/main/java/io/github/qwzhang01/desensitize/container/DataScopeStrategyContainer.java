package io.github.qwzhang01.desensitize.container;

import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.scope.DataScopeStrategy;

import java.util.concurrent.ConcurrentHashMap;

public class DataScopeStrategyContainer {
    private static final ConcurrentHashMap<Class<? extends DataScopeStrategy<?>>, DataScopeStrategy<?>> ALGO_CACHE = new ConcurrentHashMap<>();

    public DataScopeStrategy<?> getStrategy(Class<? extends DataScopeStrategy<?>> strategy) {
        DataScopeStrategy<?> scopeStrategy = ALGO_CACHE.get(strategy);
        if (scopeStrategy == null) {
            scopeStrategy = SpringContextUtil.getBeanSafely(strategy);
            if (scopeStrategy == null) {
                try {
                    scopeStrategy = strategy.getConstructor().newInstance();
                } catch (Exception e) {
                    throw new DesensitizeException(e);
                }
            }
            ALGO_CACHE.putIfAbsent(strategy, scopeStrategy);
        }

        return scopeStrategy;
    }
}
