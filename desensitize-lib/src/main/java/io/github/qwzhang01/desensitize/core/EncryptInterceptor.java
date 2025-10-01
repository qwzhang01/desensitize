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
import io.github.qwzhang01.desensitize.kit.EncryptionAlgoContainer;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ResultSetHandler 拦截
 * ResultSetHandler 负责将数据库查询结果集映射为 Java 对象。拦截 ResultSetHandler 可以自定义结果集的处理逻辑。
 * <p>
 * Intercepts 拦截器
 * Signature拦截器类型设置
 * <p>
 * type 属性指定拦截器拦截的类StatementHandler 、ResultSetHandler、ParameterHandler，Executor
 * Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed) 处理增删改查
 * ParameterHandler (getParameterObject, setParameters) 设置预编译参数
 * ResultSetHandler (handleResultSets, handleOutputParameters) 处理结果
 * StatementHandler (prepare, parameterize, batch, update, query) 处理sql预编译，设置参数
 * <p>
 * method 拦截对应类的方法
 * <p>
 * args 被拦截方法的参数
 * <p>
 *
 * @author avinzhang
 */
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class)})
public class EncryptInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(EncryptInterceptor.class);
    private static final String ENCRYPT_PREFIX = "_sensitive_start_";

    // 使用 ConcurrentHashMap 替代 CopyOnWriteArraySet
    private static final Set<Class<?>> PROCESSED_CLASSES = ConcurrentHashMap.newKeySet();

    // 缓存字段信息
    private static final Map<Class<?>, List<Field>> ENCRYPT_FIELDS_CACHE = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();

        // 获取参数对象
        Object parameterObject = getParameterObject(parameterHandler);
        if (parameterObject == null) {
            return invocation.proceed();
        }

        // 检查是否需要处理
        Object targetObject = extractTargetObject(parameterObject);
        if (targetObject == null || !shouldProcess(targetObject)) {
            return invocation.proceed();
        }

        // 执行加密
        List<FieldBackup> backups = new ArrayList<>();
        try {
            encryptFields(targetObject, backups);
            return invocation.proceed();
        } finally {
            // 确保明文能够还原
            restoreFields(backups);
        }
    }

    private Object getParameterObject(ParameterHandler parameterHandler) {
        try {
            return parameterHandler.getParameterObject();
        } catch (Exception e) {
            log.warn("获取参数对象失败", e);
            return null;
        }
    }

    private Object extractTargetObject(Object parameterObject) {
        if (parameterObject instanceof MapperMethod.ParamMap paramMap) {
            return paramMap.get("et"); // MyBatis-Plus 的实体参数
        }
        return parameterObject;
    }

    private boolean shouldProcess(Object object) {
        Class<?> clazz = object.getClass();

        // 跳过已知不需要处理的类
        if (PROCESSED_CLASSES.contains(clazz)) {
            return false;
        }

        // 检查是否有加密字段
        List<Field> encryptFields = getEncryptFields(clazz);
        if (encryptFields.isEmpty()) {
            PROCESSED_CLASSES.add(clazz);
            return false;
        }

        return true;
    }

    private List<Field> getEncryptFields(Class<?> clazz) {
        return ENCRYPT_FIELDS_CACHE.computeIfAbsent(clazz, this::findEncryptFields);
    }

    private List<Field> findEncryptFields(Class<?> clazz) {
        List<Field> encryptFields = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(EncryptField.class)) {
                field.setAccessible(true);
                encryptFields.add(field);
            }
        }

        return encryptFields;
    }

    private void encryptFields(Object object, List<FieldBackup> backups) {
        List<Field> encryptFields = getEncryptFields(object.getClass());

        for (Field field : encryptFields) {
            try {
                Object value = field.get(object);
                if (value instanceof String stringValue && !stringValue.isEmpty()) {
                    // 检查是否已经加密
                    if (!stringValue.startsWith(ENCRYPT_PREFIX)) {
                        EncryptField annotation = field.getAnnotation(EncryptField.class);
                        String encrypted = EncryptionAlgoContainer.getAlgo(annotation.value()).encrypt(stringValue);
                        String finalValue = ENCRYPT_PREFIX + encrypted;

                        // 备份原值
                        backups.add(new FieldBackup(field, stringValue, object));

                        // 设置加密值
                        field.set(object, finalValue);

                        log.debug("字段 {} 已加密", field.getName());
                    }
                }
            } catch (Exception e) {
                log.error("加密字段 {} 失败", field.getName(), e);
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
    private record FieldBackup(Field field, String originalValue, Object object) { }
}
