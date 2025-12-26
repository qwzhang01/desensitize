package io.github.qwzhang01.dsecurity.interceptor;

import io.github.qwzhang01.dsecurity.kit.SqlPrint;
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
 * MyBatis interceptor for SQL statement printing in non-production environments.
 *
 * <p>This interceptor captures and logs SQL statements with their actual
 * parameter values and execution time. It only operates in non-production
 * environments (where the active profile name does not contain "prod").</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Prints complete SQL with resolved parameter values</li>
 *   <li>Records execution time for performance monitoring</li>
 *   <li>Shows affected/returned row counts</li>
 *   <li>Environment-aware (disabled in production)</li>
 * </ul>
 *
 * @author avinzhang
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class,
                        ResultHandler.class}
        ),
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class,
                        ResultHandler.class, CacheKey.class, BoundSql.class}
        ),
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class SqlPrintInterceptor implements Interceptor {
    private final static Logger log =
            LoggerFactory.getLogger(SqlPrintInterceptor.class);
    private final Environment environment;

    public SqlPrintInterceptor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = null;
        // Capture exceptions to avoid impacting business logic
        MappedStatement mappedStatement =
                (MappedStatement) invocation.getArgs()[0];
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
                // Only print SQL in non-production environments
                if (!activeProfile.contains("prod")) {
                    SqlPrint.getInstance().print(configuration, boundSql,
                            sqlId, startTime, result);
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