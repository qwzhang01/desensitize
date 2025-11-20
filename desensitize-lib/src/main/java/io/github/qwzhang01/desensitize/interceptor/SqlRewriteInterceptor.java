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

import io.github.qwzhang01.desensitize.encrypt.context.SqlRewriteContext;
import io.github.qwzhang01.desensitize.encrypt.processor.EncryptProcessor;
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
 * @author avinzhang
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
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();

        if (METHOD_PREPARE.equals(methodName)) {
            return handlePreparePhase(invocation);
        } else if (isExecutionMethod(methodName)) {
            return handleExecutionPhase(invocation);
        }

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