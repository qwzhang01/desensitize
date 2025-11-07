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

import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.container.DataScopeStrategyContainer;
import io.github.qwzhang01.desensitize.container.EncryptFieldTableContainer;
import io.github.qwzhang01.desensitize.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.kit.ParamUtil;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.kit.StringUtil;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import io.github.qwzhang01.desensitize.scope.DataScopeStrategy;
import io.github.qwzhang01.sql.tool.helper.ParserHelper;
import io.github.qwzhang01.sql.tool.model.SqlParam;
import io.github.qwzhang01.sql.tool.model.SqlTable;
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
import java.util.Properties;

/**
 * MyBatis interceptor for SQL rewriting and parameter encryption.
 *
 * <p>This interceptor handles two main responsibilities:</p>
 * <ol>
 *   <li><strong>Parameter Encryption:</strong> Automatically encrypts query parameters for encrypted fields</li>
 *   <li><strong>Data Scope Control:</strong> Injects WHERE clauses and JOINs for data permission control</li>
 * </ol>
 *
 * <p><strong>MyBatis Interceptor Execution Order:</strong></p>
 * <pre>
 * 1. Executor          - Overall execution logic (update, query)
 * 2. StatementHandler  - SQL preparation (this interceptor intercepts here)
 * 3. ParameterHandler  - Parameter binding
 * 4. StatementHandler  - SQL execution
 * 5. ResultSetHandler  - Result set processing
 * </pre>
 *
 * <p><strong>Intercepted Methods:</strong></p>
 * <ul>
 *   <li>{@code prepare} - Encrypts parameters and applies data scope BEFORE execution</li>
 *   <li>{@code update/query/queryCursor} - Restores original parameters AFTER execution</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Uses {@link SqlRewriteContext} with ThreadLocal
 * to maintain parameter state across method calls.</p>
 *
 * <p><strong>Design Patterns:</strong></p>
 * <ul>
 *   <li>Strategy Pattern: Different encryption algorithms per field</li>
 *   <li>Template Method: Common encryption flow with pluggable algorithms</li>
 *   <li>Context Pattern: ThreadLocal context for parameter restoration</li>
 * </ul>
 *
 * @author avinzhang
 * @see EncryptField
 * @see DataScopeHelper
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
    // Method name constants for better maintainability
    private static final String METHOD_PREPARE = "prepare";
    private static final String METHOD_UPDATE = "update";
    private static final String METHOD_QUERY = "query";
    private static final String METHOD_QUERY_CURSOR = "queryCursor";

    /**
     * Intercepts StatementHandler methods to apply SQL rewriting and parameter encryption.
     *
     * <p>This method implements a two-phase approach:</p>
     * <ol>
     *   <li><strong>Prepare Phase:</strong> Encrypt parameters and apply data scope</li>
     *   <li><strong>Execute Phase:</strong> Restore original parameters after execution</li>
     * </ol>
     *
     * @param invocation the method invocation details
     * @return the result of the method execution
     * @throws Throwable if the underlying operation fails
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();

        if (METHOD_PREPARE.equals(methodName)) {
            return handlePreparePhase(invocation);
        } else if (isExecutionMethod(methodName)) {
            return handleExecutionPhase(invocation);
        }

        // For other methods, just proceed without intervention
        return invocation.proceed();
    }

    /**
     * Handles the prepare phase where SQL is prepared and parameters are encrypted.
     *
     * @param invocation the method invocation
     * @return the result of proceeding with the invocation
     * @throws Throwable if the operation fails
     */
    private Object handlePreparePhase(Invocation invocation) throws Throwable {
        // Clear any previous restoration context
        SqlRewriteContext.clear();

        // Apply parameter encryption
        encryptParameters(invocation);

        // Apply data scope if enabled
        if (Boolean.TRUE.equals(DataScopeHelper.isStarted())) {
            applyDataScope(invocation);
        }

        return invocation.proceed();
    }

    /**
     * Handles the execution phase where SQL is executed and parameters are restored.
     *
     * @param invocation the method invocation
     * @return the result of the execution
     * @throws Throwable if the operation fails
     */
    private Object handleExecutionPhase(Invocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } finally {
            // Always restore parameters to their original state
            SqlRewriteContext.restore();
        }
    }

    /**
     * Checks if the method name is an execution method (update/query/queryCursor).
     *
     * @param methodName the method name to check
     * @return true if it's an execution method
     */
    private boolean isExecutionMethod(String methodName) {
        return METHOD_UPDATE.equalsIgnoreCase(methodName)
                || METHOD_QUERY.equals(methodName)
                || METHOD_QUERY_CURSOR.equals(methodName);
    }

    /**
     * Encrypts query parameters for encrypted fields.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Parses the SQL to identify tables and parameters</li>
     *   <li>Analyzes parameter objects to find encrypted fields</li>
     *   <li>Encrypts the parameters using configured algorithms</li>
     *   <li>Saves restoration info to ThreadLocal for later recovery</li>
     * </ol>
     *
     * @param invocation the method invocation containing SQL and parameters
     */
    private void encryptParameters(Invocation invocation) {
        try {
            EncryptFieldTableContainer container = SpringContextUtil.getBean(EncryptFieldTableContainer.class);
            if (!container.hasEncrypt()) {
                // 没有注解加密字段无需走这个拦截器
                // return;
            }
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
            List<SqlTable> tables = null;
            List<SqlParam> param = null;
            try {
                tables = ParserHelper.getTables(originalSql);
                param = ParserHelper.getParam(originalSql);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (param == null || param.isEmpty() || tables.isEmpty()) {
                log.debug("未找到表信息，跳过加密处理");
                return;
            }

            // 2. 解析参数对象，获取需要加密的参数
            List<ParameterEncryptInfo> encryptInfos = ParamUtil.analyzeParameters(
                    boundSql.getParameterMappings(), param, tables, parameterObject);

            // 3. 执行参数加密
            if (!encryptInfos.isEmpty()) {
                ParamUtil.encryptParameters(encryptInfos);
                log.debug("完成参数加密，共处理 {} 个参数", encryptInfos.size());
            }
        } catch (Exception e) {
            log.error("查询参数加密处理失败", e);
        }
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
    private void applyDataScope(Invocation invocation) throws NoSuchFieldException, IllegalAccessException {
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