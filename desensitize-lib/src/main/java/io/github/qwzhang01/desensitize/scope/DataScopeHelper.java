package io.github.qwzhang01.desensitize.scope;


import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 初始化数据权限工具类
 *
 * @author avinzhang
 */
public class DataScopeHelper {
    private static final ThreadLocal<Context<?>> CONTEXT = new ThreadLocal<>();

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
     * @return
     */
    public static Context<?> strategy(Class<? extends DataScopeStrategy> strategy) {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setDataScopeFlag(true);
            CONTEXT.set(context);
        }

        context.setStrategy(strategy);
        return context;
    }

    public static Context right(List right) {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setAllRight(right);
            CONTEXT.set(context);
        }

        context.setAllRight(right);
        return context;
    }

    public static Context topTight(List right) {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setTopRight(right);
            CONTEXT.set(context);
        }

        context.setTopRight(right);
        return context;
    }

    public static Context inTight(List right) {
        Context context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setInTight(right);
            CONTEXT.set(context);
        }

        context.setInTight(right);
        return context;
    }

    /**
     * 获取线程共享变量 数据权限策略
     *
     * @return
     */
    public static Class<? extends DataScopeStrategy> getStrategy() {
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
    }

    /**
     * 执行查询
     *
     * @param function
     * @param <R>
     * @return
     */
    public static <R> R query(Callable<R> function) {
        Context<?> context = CONTEXT.get();
        if (context == null) {
            context = new Context<>();
            context.setDataScopeFlag(false);
            CONTEXT.set(context);
        }
        return context.query(function);
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
        private List<T> inTight;
        /**
         * 数据权限查询策略
         */
        private Class<? extends DataScopeStrategy> strategy;

        public Boolean getDataScopeFlag() {
            return dataScopeFlag;
        }

        public void setDataScopeFlag(Boolean dataScopeFlag) {
            this.dataScopeFlag = dataScopeFlag;
        }

        public List<T> getAllRight() {
            return allRight;
        }

        public void setAllRight(List<T> allRight) {
            this.allRight = allRight;
        }

        public List<T> getTopRight() {
            return topRight;
        }

        public void setTopRight(List<T> topRight) {
            this.topRight = topRight;
        }

        public List<T> getInTight() {
            return inTight;
        }

        public void setInTight(List<T> inTight) {
            this.inTight = inTight;
        }

        public Class<? extends DataScopeStrategy> getStrategy() {
            return strategy;
        }

        public Context<T> setStrategy(Class<? extends DataScopeStrategy> strategy) {
            this.strategy = strategy;
            return this;
        }

        public <R> R query(Callable<R> function) {
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
