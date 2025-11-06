package io.github.qwzhang01.desensitize.scope;


import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import io.github.qwzhang01.desensitize.container.DataScopeStrategyContainer;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 初始化数据权限工具类
 *
 * @author avinzhang
 */
public class DataScopeHelper {
    private static final ThreadLocal<Context<?>> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Context<?>> CONTEXT_CACHE = new ThreadLocal<>();

    /**
     * 判断数据权限是否开启
     *
     * @return
     */
    public static boolean isStarted() {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            return false;
        }
        return context.getDataScopeFlag();
    }

    /**
     * 设置线程共享变量 数据权限策略
     *
     * @param strategy 数据权限策略类
     * @param <T> 权限数据类型
     * @return 上下文对象
     */
    public static <T> Context<T> strategy(Class<? extends DataScopeStrategy<T>> strategy) {
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

    /**
     * 设置全部权限数据
     *
     * @param right 权限数据列表
     * @param <T> 权限数据类型
     * @return 上下文对象
     */
    public static <T> Context<T> right(List<T> right) {
        @SuppressWarnings("unchecked")
        Context<T> context = (Context<T>) CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setAllRight(right);
            CONTEXT.set(context);
        }

        context.setAllRight(right);
        return context;
    }

    /**
     * 设置顶部权限数据
     *
     * @param right 权限数据列表
     * @param <T> 权限数据类型
     * @return 上下文对象
     */
    public static <T> Context<T> topTight(List<T> right) {
        @SuppressWarnings("unchecked")
        Context<T> context = (Context<T>) CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setTopRight(right);
            CONTEXT.set(context);
        }

        context.setTopRight(right);
        return context;
    }

    /**
     * 设置内部权限数据
     *
     * @param right 权限数据列表
     * @param <T> 权限数据类型
     * @return 上下文对象
     */
    public static <T> Context<T> inTight(List<T> right) {
        @SuppressWarnings("unchecked")
        Context<T> context = (Context<T>) CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setInRight(right);
            CONTEXT.set(context);
        }

        context.setInRight(right);
        return context;
    }

    /**
     * 获取线程共享变量 数据权限策略
     *
     * @return 数据权限策略类
     */
    public static Class<? extends DataScopeStrategy<?>> getStrategy() {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            return null;
        }
        return context.getStrategy();
    }

    /**
     * 清除线程共享数据权限变量
     */
    public static void clear() {
        CONTEXT.remove();
        CONTEXT_CACHE.remove();
    }

    /**
     * 执行查询
     *
     * @param function
     * @param <R>
     * @return
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
     * 数据权限信息
     */
    public static final class Context<T> {
        /**
         * 数据权限开关
         */
        private Boolean dataScopeFlag;
        /**
         * 全部权限数据
         */
        private List<T> allRight;
        /**
         * 顶部查询条件，即以 topRight 为最大的查询条件
         */
        private List<T> topRight;
        /**
         * 内部查询条件，即在权限的基础上，查询存在 inTight 的数据
         */
        private List<T> inRight;

        private List<T> validRights;
        /**
         * 数据权限查询策略
         */
        private Class<? extends DataScopeStrategy<T>> strategy;

        public List<T> getValidRights() {
            return validRights;
        }

        public Context<T> setValidRights(T validRight) {
            if (validRights == null) {
                validRights = new ArrayList<>();
            }
            validRights.add(validRight);
            return this;
        }

        public Context<T> setValidRights(List<T> validRights) {
            if (this.validRights == null) {
                this.validRights = new ArrayList<>();
            }
            this.validRights.addAll(validRights);
            return this;
        }

        public Boolean getDataScopeFlag() {
            return dataScopeFlag;
        }

        public Context<T> setDataScopeFlag(Boolean dataScopeFlag) {
            this.dataScopeFlag = dataScopeFlag;
            return this;
        }

        public List<T> getAllRight() {
            return allRight;
        }

        public Context<T> setAllRight(List<T> allRight) {
            this.allRight = allRight;
            return this;
        }

        public List<T> getTopRight() {
            return topRight;
        }

        public Context<T> setTopRight(List<T> topRight) {
            this.topRight = topRight;
            return this;
        }

        public List<T> getInRight() {
            return inRight;
        }

        public Context<T> setInRight(List<T> inRight) {
            this.inRight = inRight;
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
            DataScopeStrategyContainer container = SpringContextUtil.getBean(DataScopeStrategyContainer.class);
            DataScopeStrategy<?> obj = container.getStrategy(strategy);
            // 由于容器返回的是通配符类型，这里需要进行类型转换
            @SuppressWarnings("unchecked")
            DataScopeStrategy<T> typedObj = (DataScopeStrategy<T>) obj;
            typedObj.validDs(this.validRights);

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
