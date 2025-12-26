package io.github.qwzhang01.dsecurity.scope;


import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import io.github.qwzhang01.dsecurity.kit.SpringContextUtil;
import io.github.qwzhang01.dsecurity.scope.container.DataScopeStrategyContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Data scope helper for fine-grained data access control.
 *
 * <p>This utility provides thread-local storage for data scope context,
 * enabling row-level data filtering based on user permissions or business rules.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Thread-safe context management</li>
 *   <li>Strategy pattern support for custom access rules</li>
 *   <li>Integration with SQL rewriting for transparent filtering</li>
 *   <li>Support for both search and validation scenarios</li>
 * </ul>
 *
 * @author avinzhang
 */
public class DataScopeHelper {
    private static final ThreadLocal<Context<?>> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Context<?>> CONTEXT_CACHE =
            new ThreadLocal<>();

    /**
     * Checks if data scope is enabled.
     *
     * @return true if data scope is active
     */
    public static boolean isStarted() {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            return false;
        }
        return context.getDataScopeFlag();
    }

    /**
     * Sets thread-local data scope strategy.
     *
     * @param strategy data scope strategy class
     * @param <T>      permission data type
     * @return context object for method chaining
     */
    public static <T> Context<T> strategy(Class<?
            extends DataScopeStrategy<T>> strategy) {
        @SuppressWarnings("unchecked")
        Context<T> context = (Context<T>) CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setDataScopeFlag(true);
            CONTEXT.set(context);
        }

        context.setStrategy(strategy);
        return context;
    }

    public static <T> List<T> getSearchRight() {
        @SuppressWarnings("unchecked")
        Context<T> context = (Context<T>) CONTEXT.get();
        if (context == null) {
            context = (Context<T>) CONTEXT_CACHE.get();
            if (context == null) {
                return null;
            }
        }
        return context.getSearchRight();
    }

    /**
     * Gets thread-local data scope strategy.
     *
     * @return data scope strategy class
     */
    public static Class<? extends DataScopeStrategy<?>> getStrategy() {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            return null;
        }
        return context.getStrategy();
    }

    /**
     * Clears thread-local data scope variables.
     */
    public static void clear() {
        CONTEXT.remove();
        CONTEXT_CACHE.remove();
    }

    /**
     * Executes a query with data scope applied.
     *
     * @param function the query function to execute
     * @param <R>      the return type
     * @return the query result
     */
    public static <R> R execute(Callable<R> function) {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setDataScopeFlag(false);
            CONTEXT.set(context);
        }
        return context.execute(function);
    }

    public static void cache() {
        Context<?> context = CONTEXT.get();
        if (context != null) {
            CONTEXT_CACHE.set(context);
            CONTEXT.remove();
        }
    }

    public static void restore() {
        Context<?> context = CONTEXT_CACHE.get();
        if (context != null) {
            CONTEXT.set(context);
            CONTEXT_CACHE.remove();
        }
    }

    /**
     * Data scope context information.
     */
    public static final class Context<T> {
        /**
         * Data scope enable flag
         */
        private Boolean dataScopeFlag;
        /**
         * Search conditions (used as data scope filter)
         */
        private List<T> searchRight;
        /**
         * Validation rights (used in INSERT/UPDATE/DELETE operations)
         */
        private List<T> validRights;
        /**
         * Excluded rights (whitelist for INSERT/UPDATE/DELETE operations)
         */
        private List<T> withoutRights;
        /**
         * Data scope query strategy
         */
        private Class<? extends DataScopeStrategy<T>> strategy;

        public List<T> getSearchRight() {
            return searchRight;
        }

        public Context<T> setSearchRight(T searchRight) {
            if (this.searchRight == null) {
                this.searchRight = new ArrayList<>();
            }
            this.searchRight.add(searchRight);
            return this;
        }

        public Context<T> setSearchRight(List<T> searchRight) {
            if (this.searchRight == null) {
                this.searchRight = new ArrayList<>();
            }
            this.searchRight.addAll(searchRight);
            return this;
        }

        public List<T> getValidRights() {
            return validRights;
        }

        public Context<T> setValidRights(T validRight) {
            if (this.validRights == null) {
                this.validRights = new ArrayList<>();
            }
            this.validRights.add(validRight);
            return this;
        }

        public Context<T> setValidRights(List<T> validRights) {
            if (this.validRights == null) {
                this.validRights = new ArrayList<>();
            }
            this.validRights.addAll(validRights);
            return this;
        }

        public List<T> getWithoutRights() {
            return withoutRights;
        }

        public Context<T> setWithoutRights(T withoutRight) {
            if (this.withoutRights == null) {
                this.withoutRights = new ArrayList<>();
            }
            this.withoutRights.add(withoutRight);
            return this;
        }

        public Context<T> setWithoutRights(List<T> withoutRights) {
            if (this.withoutRights == null) {
                this.withoutRights = new ArrayList<>();
            }
            this.withoutRights.addAll(withoutRights);
            return this;
        }

        public Boolean getDataScopeFlag() {
            return dataScopeFlag;
        }

        public Context<T> setDataScopeFlag(Boolean dataScopeFlag) {
            this.dataScopeFlag = dataScopeFlag;
            return this;
        }

        public Class<? extends DataScopeStrategy<T>> getStrategy() {
            return strategy;
        }

        public Context<T> setStrategy(Class<? extends DataScopeStrategy<T>> strategy) {
            this.strategy = strategy;
            return this;
        }

        public <R> R execute(Callable<R> function) {
            DataScopeStrategyContainer container =
                    SpringContextUtil.getBean(DataScopeStrategyContainer.class);
            DataScopeStrategy<?> obj = container.getStrategy(strategy);
            // 由于容器返回的是通配符类型，这里需要进行类型转换
            @SuppressWarnings("unchecked")
            DataScopeStrategy<T> typedObj = (DataScopeStrategy<T>) obj;
            typedObj.validDs(this.validRights);
            typedObj.validDs(this.validRights, this.withoutRights);

            try {
                return function.call();
            } catch (Exception e) {
                throw new MybatisPlusException(e);
            } finally {
                clear();
            }
        }
    }
}