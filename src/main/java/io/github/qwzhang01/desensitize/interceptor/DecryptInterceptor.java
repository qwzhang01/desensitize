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
import io.github.qwzhang01.desensitize.encrypt.container.AbstractEncryptAlgoContainer;
import io.github.qwzhang01.desensitize.encrypt.processor.DecryptProcessor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.List;

/**
 * MyBatis interceptor for automatic decryption of query results.
 *
 * <p>This interceptor intercepts {@link ResultSetHandler#handleResultSets} to automatically
 * decrypt encrypted fields in query results. It processes both single objects and lists,
 * finding fields annotated with {@link EncryptField} and applying the configured decryption
 * algorithm.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Automatic detection of encrypted fields via @EncryptField annotation</li>
 *   <li>Support for both selectOne and selectList operations</li>
 *   <li>Multiple encryption algorithm support through Strategy Pattern</li>
 *   <li>Thread-safe operation</li>
 * </ul>
 *
 * <p><strong>Execution Flow:</strong></p>
 * <pre>
 * 1. Query executes and returns encrypted results
 * 2. Interceptor captures the result set
 * 3. Scans for @EncryptField annotated fields
 * 4. Applies appropriate decryption algorithm
 * 5. Returns decrypted data to caller
 * </pre>
 *
 * <p><strong>Performance Considerations:</strong></p>
 * <ul>
 *   <li>Uses reflection caching to minimize overhead</li>
 *   <li>Only processes fields with encryption annotations</li>
 *   <li>Skips processing if no encrypted fields are found</li>
 * </ul>
 *
 * @author avinzhang
 * @see EncryptField
 * @see AbstractEncryptAlgoContainer
 */
@Intercepts({
        @Signature(
                type = ResultSetHandler.class,
                method = "handleResultSets",
                args = {Statement.class}
        )
})
public class DecryptInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DecryptInterceptor.class);

    /**
     * Intercepts query result handling to decrypt encrypted fields.
     *
     * <p>This method is called after a query executes but before results are returned
     * to the application. It processes both single results (selectOne) and lists (selectList).</p>
     *
     * @param invocation the method invocation details
     * @return the result object with decrypted fields
     * @throws Throwable if the underlying operation fails
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Execute the original query
        Object resultObject = invocation.proceed();

        if (resultObject == null) {
            log.debug("Query returned null, skipping decryption");
            return null;
        }

        // Process results based on type
        if (resultObject instanceof List<?> resultList) {
            DecryptProcessor.getInstance().decryptList(resultList);
        } else {
            DecryptProcessor.getInstance().decryptSingle(resultObject);
        }

        return resultObject;
    }


    /**
     * Wraps the target object with this interceptor.
     * Only wraps if the target is a ResultSetHandler.
     *
     * @param target the target object to potentially wrap
     * @return the wrapped target or the original target
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof ResultSetHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }
}

