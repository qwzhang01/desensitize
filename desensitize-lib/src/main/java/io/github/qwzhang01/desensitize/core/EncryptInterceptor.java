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
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.EncryptionAlgoContainer;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * MyBatis ParameterHandler interceptor for automatic field encryption.
 * <p>
 * This interceptor automatically encrypts fields marked with @EncryptField annotation
 * before SQL execution and restores original values after processing.
 * <p>
 * MyBatis Interceptor Types:
 * - Executor: handles CRUD operations (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
 * - ParameterHandler: handles parameter setting (getParameterObject, setParameters)
 * - ResultSetHandler: handles result processing (handleResultSets, handleOutputParameters)
 * - StatementHandler: handles SQL preparation and parameter setting (prepare, parameterize, batch, update, query)
 * <p>
 * This interceptor targets ParameterHandler.setParameters method to encrypt parameters
 * before they are set in the prepared statement.
 *
 * @author avinzhang
 */
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})})
public class EncryptInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(EncryptInterceptor.class);

    // Use ConcurrentHashMap instead of CopyOnWriteArraySet for better performance
    private static final Set<Class<?>> PROCESSED_CLASSES = ConcurrentHashMap.newKeySet();

    // Cache field information for performance optimization
    private static final Map<Class<?>, List<Field>> ENCRYPT_FIELDS_CACHE = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();

        // Get parameter object
        Object parameterObject = parameterHandler.getParameterObject();
        if (parameterObject == null) {
            return invocation.proceed();
        }

        Set<Object> set = new HashSet<>();
        // Execute encryption
        List<FieldBackup> backups = new ArrayList<>();
        try {
            if (parameterObject instanceof java.util.List) {
                for (Object item : (java.util.List<?>) parameterObject) {
                    if (!set.contains(item)) {
                        encryptFields(item, backups);
                        set.add(item);
                    }
                }
            } else if (parameterObject instanceof Map<?, ?>) {
                for (Object item : ((Map<?, ?>) parameterObject).values()) {
                    if (!set.contains(item)) {
                        encryptFields(item, backups);
                        set.add(item);
                    }
                }
            } else if (parameterObject instanceof Set<?>) {
                for (Object item : (Set<?>) parameterObject) {
                    if (!set.contains(item)) {
                        encryptFields(item, backups);
                        set.add(item);
                    }
                }
            } else {
                if (!set.contains(parameterObject)) {
                    encryptFields(parameterObject, backups);
                    set.add(parameterObject);
                }
            }

            return invocation.proceed();
        } finally {
            // Ensure plaintext can be restored
            restoreFields(backups);
            set.clear();
            set = null;
        }
    }

    private void encryptFields(Object obj, List<FieldBackup> backups) {
        if (obj == null) {
            return;
        }

        List<ClazzUtil.AnnotatedFieldResult<EncryptField>> fields = ClazzUtil.getAnnotatedFields(obj, EncryptField.class);
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }

        for (ClazzUtil.AnnotatedFieldResult<EncryptField> fieldObj : fields) {
            try {
                EncryptField annotation = fieldObj.annotation();
                Field field = fieldObj.field();
                Object object = fieldObj.containingObject();
                Object value = fieldObj.getFieldValue();

                field.setAccessible(true);
                if (value instanceof String strValue) {
                    if (StringUtils.hasText(strValue)) {
                        // 备份原值
                        backups.add(new FieldBackup(field, strValue, object));
                        strValue = EncryptionAlgoContainer.getAlgo(annotation.value()).encrypt(strValue);
                        field.set(object, strValue);
                        log.debug("字段 {} 已加密", field.getName());
                    }
                }
            } catch (Exception e) {
                log.error("加密字段 {} 失败", fieldObj.field().getName(), e);
            }
        }
    }

    private void restoreFields(List<FieldBackup> backups) {
        for (FieldBackup backup : backups) {
            try {
                backup.field.set(backup.object, backup.originalValue);
                log.debug("字段 {} 已还原", backup.field.getName());
            } catch (Exception e) {
                log.error("还原字段 {} 失败", backup.field.getName(), e);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 字段备份内部类
     */
    private record FieldBackup(Field field, String originalValue, Object object) {
    }
}
