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
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    /**
     * 判断数据权限是否开启
     *
     * @return
     */
    public static boolean isStarted() {
        Context context = CONTEXT.get();
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
    @SafeVarargs
    public static Context setStrategy(Class<? extends DataScopeStrategy>... strategy) {
        Context context = CONTEXT.get();
        if (context == null) {
            context = new Context();
            context.setDataScopeFlag(true);
            CONTEXT.set(context);
        }

        context.setStrategy(strategy);

        if (context.getStrategy() == null || context.getStrategy().length == 0) {
            context.setDataScopeFlag(false);
        }

        return context;
    }

    public static Context get() {
        return CONTEXT.get();
    }

    public static void set(Context context) {
        if (context == null) {
            return;
        }
        CONTEXT.set(context);
    }

    /**
     * 获取线程共享变量 数据权限策略
     *
     * @return
     */
    public static Class<? extends DataScopeStrategy>[] getStrategy() {
        Context context = CONTEXT.get();
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
        Context context = CONTEXT.get();
        if (context == null) {
            context = new Context();
            context.setDataScopeFlag(false);
            CONTEXT.set(context);
        }
        return context.query(function);
    }

    /**
     * 数据权限信息
     */
    public static final class Context {
        private Boolean dataScopeFlag;
        private Class<? extends DataScopeStrategy>[] strategy;
        private List<DataScopeStrategy.Where> wheres;

        public Boolean getDataScopeFlag() {
            return dataScopeFlag;
        }

        public void setDataScopeFlag(Boolean dataScopeFlag) {
            this.dataScopeFlag = dataScopeFlag;
        }

        public Class<? extends DataScopeStrategy>[] getStrategy() {
            return strategy;
        }

        public Context setStrategy(Class<? extends DataScopeStrategy>[] strategy) {
            this.strategy = strategy;
            return this;
        }

        public List<DataScopeStrategy.Where> getWheres() {
            return wheres;
        }

        public void setWheres(List<DataScopeStrategy.Where> wheres) {
            this.wheres = wheres;
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
