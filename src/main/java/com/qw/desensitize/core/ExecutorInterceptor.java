package com.qw.desensitize.core;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;

import java.util.Properties;

/**
 * ExecutorInterceptor
 * Executor 是 MyBatis 的核心执行器，负责管理数据库操作（如查询、更新、事务管理等）。
 * 拦截 Executor 可以控制整个 SQL 执行流程，包括查询、更新、插入、删除以及事务相关操作。
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        )
})
public class ExecutorInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long timeTaken = System.currentTimeMillis() - startTime;
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            logger.debug(() -> "SQL [" + ms.getId() + "] 执行耗时: " + timeTaken + "ms");
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}