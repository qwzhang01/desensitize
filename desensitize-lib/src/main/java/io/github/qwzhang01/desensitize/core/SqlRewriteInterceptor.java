/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.qwzhang01.desensitize.core;

import io.github.qwzhang01.desensitize.container.DataScopeStrategyContainer;
import io.github.qwzhang01.desensitize.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.exception.DataScopeErrorException;
import io.github.qwzhang01.desensitize.kit.ParamUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.kit.StringUtil;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import io.github.qwzhang01.desensitize.scope.DataScopeStrategy;
import io.github.qwzhang01.sql.tool.helper.SqlGatherHelper;
import io.github.qwzhang01.sql.tool.helper.SqlParseHelper;
import io.github.qwzhang01.sql.tool.model.SqlCondition;
import io.github.qwzhang01.sql.tool.model.SqlField;
import io.github.qwzhang01.sql.tool.model.SqlGather;
import io.github.qwzhang01.sql.tool.model.SqlJoin;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 拦截器执行顺序
 * Executor：首先执行，负责整体执行逻辑（如update、query）。
 * StatementHandler（prepare阶段）：其次执行，准备SQL语句。
 * ParameterHandler：然后执行，处理参数绑定。
 * StatementHandler（execute阶段）：再次执行，实际执行SQL。
 * ResultSetHandler：最后执行，处理结果集
 * <p>
 * StatementHandler 拦截器 - 纯 MyBatis 版本
 * 负责处理 SQL 语句的预编译和执行，实现查询参数的自动加密
 */
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        ),
        @Signature(
                type = StatementHandler.class,
                method = "update",
                args = {Statement.class}
        ),
        @Signature(
                type = StatementHandler.class,
                method = "query",
                args = {Statement.class, ResultHandler.class}
        ),
        @Signature(type = StatementHandler.class,
                method = "queryCursor",
                args = {Statement.class})

})
public class SqlRewriteInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlRewriteInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if ("prepare".equals(methodName)) {
            // 清理之前的还原信息
            SqlRewriteContext.clear();

            // 处理加密解密逻辑
            encrypt(invocation);

            boolean started = DataScopeHelper.isStarted();
            if (!Boolean.TRUE.equals(started)) {
                return invocation.proceed();
            }
            // 处理加密解密逻辑
            dataScope(invocation);
            return invocation.proceed();
        } else if ("update".equalsIgnoreCase(methodName) || "query".equals(methodName) || "queryCursor".equals(methodName)) {
            try {
                return invocation.proceed();
            } finally {
                SqlRewriteContext.restore();
            }
        }
        return invocation.proceed();
    }

    /**
     * 查询参数加密处理（纯 MyBatis 版本）
     * 加密完成后会保存还原信息，在 SQL 执行完成后自动还原明文
     */
    private void encrypt(Invocation invocation) {
        try {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            // 获取 ParameterHandler 中的参数对象
            Object parameterObject = statementHandler.getParameterHandler().getParameterObject();
            BoundSql boundSql = statementHandler.getBoundSql();

            String originalSql = boundSql.getSql();
            log.debug("开始处理查询加密，SQL: {}", originalSql);

            if (boundSql.getParameterObject() == null) {
                log.debug("参数对象为空，跳过加密处理");
                return;
            }

            // 1. 解析 SQL 获取所有涉及的表信息
            SqlGather sqlAnalysis = null;
            try {
                sqlAnalysis = SqlGatherHelper.analysis(originalSql);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (sqlAnalysis == null || sqlAnalysis.getTables() == null || sqlAnalysis.getTables().isEmpty()) {
                log.warn("未找到表信息，跳过加密处理");
                return;
            }

            // 2. 解析参数对象，获取需要加密的参数
            List<ParameterEncryptInfo> encryptInfos = ParamUtil.analyzeParameters(
                    boundSql, sqlAnalysis, parameterObject);

            // 3. 执行参数加密
            if (!encryptInfos.isEmpty()) {
                ParamUtil.encryptParameters(encryptInfos);
                log.debug("完成参数加密，共处理 {} 个参数", encryptInfos.size());
            }
        } catch (Exception e) {
            log.error("查询参数加密处理失败", e);
        }
    }

    /*** 数据权限逻辑 */
    private void dataScope(Invocation invocation) throws NoSuchFieldException, IllegalAccessException {
        boolean started = DataScopeHelper.isStarted();
        Class<? extends DataScopeStrategy> strategy = DataScopeHelper.getStrategy();
        if (!started && strategy != null) {
            return;
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        // 获取原始 SQL
        String originalSql = boundSql.getSql();
        String modifiedSql = dataScope(originalSql, strategy);
        // 使用反射修改 BoundSql 的 sql 字段
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, modifiedSql);
    }

    /*** 数据权限真正处理位置 */
    private String dataScope(String originalSql, Class<? extends DataScopeStrategy> strategy) {
        DataScopeStrategyContainer container = SpringContextUtil.getBean(DataScopeStrategyContainer.class);
        DataScopeStrategy obj = container.getStrategy(strategy);
        String join = obj.join();
        String where = obj.where();

        if (StringUtil.isEmpty(join) && StringUtil.isEmpty(where)) {
            return originalSql;
        }

        // 1. 解析 SQL 获取所有涉及的表信息
        SqlGather sqlAnalysis = null;
        try {
            sqlAnalysis = SqlGatherHelper.analysis(originalSql);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return originalSql;
        }

        if (!StringUtil.isEmpty(join)) {
            List<SqlGather.Table> tables = sqlAnalysis.getTables();
            Map<String, String> map = tables.stream().collect(Collectors.toMap(SqlGather.Table::tableName, SqlGather.Table::alias, (v1, v2) -> v1));
            List<SqlJoin> joins = SqlParseHelper.parseJoin(join);
            for (SqlJoin joinPart : joins) {
                List<SqlCondition> conditions = joinPart.getJoinConditions();
                for (SqlCondition condition : conditions) {
                    String alias = joinPart.getAlias();
                    SqlField rightFieldInfo = condition.getRightFieldInfo();
                    if (!StringUtil.isEmpty(rightFieldInfo.getTableAlias()) && !alias.equals(rightFieldInfo.getTableAlias())) {
                        rightFieldInfo.setTableName(rightFieldInfo.getTableAlias());
                        rightFieldInfo.setTableAlias(map.get(rightFieldInfo.getTableAlias()));
                        if (StringUtil.isEmpty(rightFieldInfo.getTableAlias())) {
                            throw new DataScopeErrorException("数据权限的关联表，无法与主SQL关联起来，请检查表名是否正确");
                        }
                    }
                    SqlField leftFieldInfo = condition.getFieldInfo();
                    if (!StringUtil.isEmpty(leftFieldInfo.getTableAlias()) && !alias.equals(leftFieldInfo.getTableAlias())) {
                        leftFieldInfo.setTableName(leftFieldInfo.getTableAlias());
                        leftFieldInfo.setTableAlias(map.get(leftFieldInfo.getTableAlias()));
                        if (StringUtil.isEmpty(leftFieldInfo.getTableAlias())) {
                            throw new DataScopeErrorException("数据权限的关联表，无法与主SQL关联起来，请检查表名是否正确");
                        }
                    }
                }
            }
            join = SqlParseHelper.toSQL(joins);
        }
        if (!StringUtil.isEmpty(where)) {
            List<SqlCondition> sqlConditions = SqlParseHelper.parseWhere(where);
            // todo 待处理
        }

        return originalSql;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可选：处理配置属性
        log.debug("处理配置属性: {}", properties);
    }
}