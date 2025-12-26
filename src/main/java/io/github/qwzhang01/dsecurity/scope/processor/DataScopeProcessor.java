package io.github.qwzhang01.dsecurity.scope.processor;

import io.github.qwzhang01.dsecurity.kit.SpringContextUtil;
import io.github.qwzhang01.dsecurity.kit.StringUtil;
import io.github.qwzhang01.dsecurity.scope.DataScopeHelper;
import io.github.qwzhang01.dsecurity.scope.DataScopeStrategy;
import io.github.qwzhang01.dsecurity.scope.container.DataScopeStrategyContainer;
import io.github.qwzhang01.sql.tool.helper.ParserHelper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Data scope processor for SQL modification.
 *
 * <p>This processor applies data scope (permission control) by modifying SQL
 * statements to include additional WHERE clauses and JOINs based on the
 * configured data scope strategy.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Transparent SQL rewriting for permission control</li>
 *   <li>Support for custom JOIN and WHERE conditions</li>
 *   <li>Thread-safe operation with ThreadLocal context</li>
 *   <li>Automatic context cleanup after SQL execution</li>
 * </ul>
 *
 * @author avinzhang
 */
public class DataScopeProcessor {
    private static final Logger log =
            LoggerFactory.getLogger(DataScopeProcessor.class);

    private DataScopeProcessor() {
    }

    public static DataScopeProcessor getInstance() {
        return DataScopeProcessor.Holder.INSTANCE;
    }

    /**
     * Applies data scope (permission control) by modifying the SQL.
     *
     * <p>This method injects additional WHERE clauses and JOINs into the SQL
     * based on the configured data scope strategy. It's used to implement
     * fine-grained data access control.</p>
     *
     * @param invocation the method invocation containing SQL to modify
     * @throws NoSuchFieldException   if the SQL field cannot be accessed
     * @throws IllegalAccessException if field access is denied
     */
    public void apply(Invocation invocation) throws NoSuchFieldException,
            IllegalAccessException {
        if (!Boolean.TRUE.equals(DataScopeHelper.isStarted())) {
            return;
        }

        // Apply data scope if enabled
        boolean started = DataScopeHelper.isStarted();
        Class<? extends DataScopeStrategy<?>> strategy =
                DataScopeHelper.getStrategy();
        if (!started && strategy != null) {
            return;
        }

        // Clean data scope info to avoid affecting other SQL statements
        DataScopeHelper.cache();

        StatementHandler statementHandler =
                (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        // Get original SQL
        String originalSql = boundSql.getSql();
        DataScopeStrategyContainer container =
                SpringContextUtil.getBean(DataScopeStrategyContainer.class);
        DataScopeStrategy<?> obj = container.getStrategy(strategy);

        String join = obj.join();
        String where = obj.where();

        if (!StringUtil.isEmpty(join) && !StringUtil.isEmpty(where)) {
            originalSql = ParserHelper.addJoinAndWhere(originalSql.trim(),
                    join.trim(), where.trim());
        } else if (!StringUtil.isEmpty(join)) {
            originalSql = ParserHelper.addJoin(originalSql.trim(), join.trim());
        } else if (!StringUtil.isEmpty(where)) {
            originalSql = ParserHelper.addWhere(originalSql.trim(),
                    where.trim());
        }

        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, originalSql);

        DataScopeHelper.restore();
    }

    private static final class Holder {
        private static final DataScopeProcessor INSTANCE =
                new DataScopeProcessor();
    }
}
