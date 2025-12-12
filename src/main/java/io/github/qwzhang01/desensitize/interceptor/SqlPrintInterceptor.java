package io.github.qwzhang01.desensitize.interceptor;

import io.github.qwzhang01.desensitize.kit.SqlPrint;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * 非生产环境打印SQL执行真实脚本
 *
 * @author avinzhang
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        ),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}
        ),
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class SqlPrintInterceptor implements Interceptor {
    private final static Logger log = LoggerFactory.getLogger(SqlPrintInterceptor.class);
    private final Environment environment;

    public SqlPrintInterceptor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = null;
        // 捕获掉异常，不要影响业务
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = null;
        if (invocation.getArgs().length > 1) {
            parameter = invocation.getArgs()[1];
        }
        String sqlId = mappedStatement.getId();
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        Configuration configuration = mappedStatement.getConfiguration();
        long startTime = System.currentTimeMillis();
        try {
            result = invocation.proceed();
        } finally {
            String[] activeProfiles = environment.getActiveProfiles();
            if (activeProfiles != null && activeProfiles.length > 0) {
                String activeProfile = activeProfiles[0];
                if (!activeProfile.contains("prod")) {
                    SqlPrint.getInstance().print(configuration, boundSql, sqlId, startTime, result);
                }
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

}