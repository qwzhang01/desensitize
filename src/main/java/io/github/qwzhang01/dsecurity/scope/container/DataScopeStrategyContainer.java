package io.github.qwzhang01.dsecurity.scope.container;

import io.github.qwzhang01.dsecurity.exception.DesensitizeException;
import io.github.qwzhang01.dsecurity.kit.SpringContextUtil;
import io.github.qwzhang01.dsecurity.scope.DataScopeStrategy;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Data scope strategy container for managing strategy instances.
 *
 * <p>This container manages data scope strategy instances using lazy
 * instantiation with caching. It follows the Factory Pattern to create
 * and cache strategy objects.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Thread-safe caching with ConcurrentHashMap</li>
 *   <li>Spring context integration for dependency injection</li>
 *   <li>Fallback to reflection-based instantiation</li>
 * </ul>
 *
 * @author avinzhang
 */
public class DataScopeStrategyContainer {
    private static final ConcurrentHashMap<Class<? extends DataScopeStrategy<
            ?>>, DataScopeStrategy<?>> ALGO_CACHE = new ConcurrentHashMap<>();

    public DataScopeStrategy<?> getStrategy(Class<?
            extends DataScopeStrategy<?>> strategy) {
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
