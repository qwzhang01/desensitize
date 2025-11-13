package io.github.qwzhang01.desensitize.scope.processor;

import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.kit.StringUtil;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import io.github.qwzhang01.desensitize.scope.DataScopeStrategy;
import io.github.qwzhang01.desensitize.scope.container.DataScopeStrategyContainer;
import io.github.qwzhang01.sql.tool.helper.ParserHelper;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class DataScopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(DataScopeProcessor.class);

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
    public void apply(Invocation invocation) throws NoSuchFieldException, IllegalAccessException {
        if (!Boolean.TRUE.equals(DataScopeHelper.isStarted())) {
            return;
        }

        // Apply data scope if enabled
        boolean started = DataScopeHelper.isStarted();
        Class<? extends DataScopeStrategy<?>> strategy = DataScopeHelper.getStrategy();
        if (!started && strategy != null) {
            return;
        }

        // 清理数据权限信息，避免影响其他 SQL
        DataScopeHelper.cache();

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        // 获取原始 SQL
        String originalSql = boundSql.getSql();
        DataScopeStrategyContainer container = SpringContextUtil.getBean(DataScopeStrategyContainer.class);
        DataScopeStrategy<?> obj = container.getStrategy(strategy);

        String join = obj.join();
        String where = obj.where();

        if (!StringUtil.isEmpty(join) && !StringUtil.isEmpty(where)) {
            originalSql = ParserHelper.addJoinAndWhere(originalSql.trim(), join.trim(), where.trim());
        } else if (!StringUtil.isEmpty(join)) {
            originalSql = ParserHelper.addJoin(originalSql.trim(), join.trim());
        } else if (!StringUtil.isEmpty(where)) {
            originalSql = ParserHelper.addWhere(originalSql.trim(), where.trim());
        }

        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, originalSql);

        DataScopeHelper.restore();
    }

    private static final class Holder {
        private static final DataScopeProcessor INSTANCE = new DataScopeProcessor();
    }
}
