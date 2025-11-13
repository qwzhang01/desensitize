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

package io.github.qwzhang01.desensitize.interceptor;

import io.github.qwzhang01.desensitize.encrypt.annotation.EncryptField;
import io.github.qwzhang01.desensitize.encrypt.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.encrypt.processor.EncryptProcessor;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import io.github.qwzhang01.desensitize.scope.processor.DataScopeProcessor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;
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
        EncryptProcessor.getInstance().encryptParameters(invocation);

        DataScopeProcessor.getInstance().apply(invocation);

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