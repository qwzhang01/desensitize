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

import io.github.qwzhang01.desensitize.kit.EncryptionAlgoContainer;
import io.github.qwzhang01.desensitize.kit.SpringContextUtil;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import io.github.qwzhang01.desensitize.table.TableContainer;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StatementHandler 拦截器 - 纯 MyBatis 版本
 * 负责处理 SQL 语句的预编译和执行，实现查询参数的自动加密
 */
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
public class SqlRewriteInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlRewriteInterceptor.class);

    // 表容器，用于管理加密字段信息

    // SQL 解析相关的正则表达式
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN|UPDATE|INTO)\\s+([\\w_]+)(?:\\s+(?:AS\\s+)?([\\w_]+))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WHERE_CONDITION_PATTERN = Pattern.compile(
            "(?i)\\b([\\w_]+\\.)?([\\w_]+)\\s*(?:=|!=|<>|LIKE|IN|NOT\\s+IN)\\s*\\?",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        boolean started = DataScopeHelper.isStarted();
        if (!Boolean.TRUE.equals(started)) {
            queryEncrypt(invocation);
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        // 获取原始 SQL
        String originalSql = boundSql.getSql();
        Object parameterObject = boundSql.getParameterObject();

        // 修改 SQL，添加软删除条件
        String modifiedSql = originalSql + " WHERE deleted = 0";

        // TODO 数据权限逻辑待实现
        // 使用反射修改 BoundSql 的 sql 字段
        Field field = BoundSql.class.getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, modifiedSql);

        // 继续执行
        queryEncrypt(invocation);
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可选：处理配置属性
    }

    /**
     * 查询参数加密处理（纯 MyBatis 版本）
     */
    private void queryEncrypt(Invocation invocation) {
        try {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = statementHandler.getBoundSql();

            String originalSql = boundSql.getSql();
            log.debug("开始处理查询加密，SQL: {}", originalSql);

            if (boundSql.getParameterObject() == null) {
                log.debug("参数对象为空，跳过加密处理");
                return;
            }

            // 1. 解析 SQL 获取所有涉及的表信息
            SqlAnalysisResult sqlAnalysis = analyzeSql(originalSql);
            if (sqlAnalysis.getTables().isEmpty()) {
                log.debug("未找到表信息，跳过加密处理");
                return;
            }


            // 获取 ParameterHandler 中的参数对象
            Object parameterObject = statementHandler.getParameterHandler().getParameterObject();

            // 获取参数映射列表
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            // 如果有参数映射，遍历并获取对应的值
            if (parameterMappings != null && !parameterMappings.isEmpty()) {
                MetaObject metaObject = SystemMetaObject.forObject(parameterObject);
                for (ParameterMapping mapping : parameterMappings) {
                    String propertyName = mapping.getProperty();
                    Object value = null;

                    try {
                        // 统一使用 MetaObject 来获取属性值，支持嵌套属性
                        if (metaObject.hasGetter(propertyName)) {
                            value = metaObject.getValue(propertyName);
                        } else if (parameterObject instanceof Map) {
                            // 如果 MetaObject 无法获取，尝试直接从 Map 获取
                            value = ((Map<?, ?>) parameterObject).get(propertyName);
                        } else {
                            // 简单类型直接取值
                            value = parameterObject;
                        }
                    } catch (Exception e) {
                        log.debug("获取参数值失败: {}", propertyName, e);
                        // 如果 MetaObject 失败，尝试直接从 Map 获取
                        if (parameterObject instanceof Map) {
                            value = ((Map<?, ?>) parameterObject).get(propertyName);
                        }
                    }

                    System.out.println("参数 [" + propertyName + "] 的值: " + value);
                }
            } else {
                // 没有参数映射，直接打印参数对象
                System.out.println("参数对象: " + parameterObject);
            }


            // 2. 解析参数对象，获取需要加密的参数
            List<ParameterEncryptInfo> encryptInfos = analyzeParameters(
                    boundSql, sqlAnalysis, parameterObject);

            // 3. 执行参数加密
            if (!encryptInfos.isEmpty()) {
                encryptParameters(encryptInfos);
                log.debug("完成参数加密，共处理 {} 个参数", encryptInfos.size());
            }

        } catch (Exception e) {
            log.error("查询参数加密处理失败", e);
        }
    }

    /**
     * 分析 SQL 语句，提取表信息和字段信息
     */
    private SqlAnalysisResult analyzeSql(String sql) {
        SqlAnalysisResult result = new SqlAnalysisResult();

        // 解析表名和别名
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String alias = tableMatcher.group(2);

            if ("WHERE".equalsIgnoreCase(alias.trim()) || "ON".equalsIgnoreCase(alias.trim())) {
                alias = "";
            }

            TableInfo tableInfo = new TableInfo(tableName, alias);
            result.addTable(tableInfo);

            log.debug("发现表: {} (别名: {})", tableName, alias);
        }

        // 解析 WHERE 条件中的字段
        Matcher conditionMatcher = WHERE_CONDITION_PATTERN.matcher(sql);
        while (conditionMatcher.find()) {
            String tableAlias = conditionMatcher.group(1);
            String columnName = conditionMatcher.group(2);

            if (tableAlias != null) {
                tableAlias = tableAlias.replace(".", "");
            }

            FieldCondition condition = new FieldCondition(tableAlias, columnName);
            result.addCondition(condition);

            log.debug("发现查询条件字段: {}.{}", tableAlias, columnName);
        }

        return result;
    }

    /**
     * 分析参数对象，确定需要加密的参数
     */
    private List<ParameterEncryptInfo> analyzeParameters(BoundSql boundSql,
                                                         SqlAnalysisResult sqlAnalysis,
                                                         Object parameterObject) {
        List<ParameterEncryptInfo> encryptInfos = new ArrayList<>();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        log.debug("参数对象类型: {}, 参数映射数量: {}",
                parameterObject.getClass().getSimpleName(), parameterMappings.size());

        if (parameterObject instanceof Map) {
            // 处理 Map 参数（MyBatis 常见场景）
            analyzeMapParameters((Map<String, Object>) parameterObject,
                    parameterMappings, sqlAnalysis, encryptInfos);
        } else {
            // 处理实体对象参数
            analyzeObjectParameters(parameterObject, parameterMappings,
                    sqlAnalysis, encryptInfos);
        }

        return encryptInfos;
    }

    /**
     * 分析 Map 类型的参数
     */
    private void analyzeMapParameters(Map<String, Object> paramMap,
                                      List<ParameterMapping> parameterMappings,
                                      SqlAnalysisResult sqlAnalysis,
                                      List<ParameterEncryptInfo> encryptInfos) {

        log.debug("分析 Map 参数: {}", paramMap.keySet());

        // 创建 MetaObject 来处理嵌套属性
        MetaObject metaObject = SystemMetaObject.forObject(paramMap);

        // 方法1：通过 ParameterMapping 分析
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();
            Object value = null;

            try {
                // 使用 MetaObject 获取嵌套属性值，支持 userParam.phoneNo 这样的路径
                if (metaObject.hasGetter(property)) {
                    value = metaObject.getValue(property);
                } else {
                    // 回退到直接从 Map 获取
                    value = paramMap.get(property);
                }

                if (value instanceof String) {
                    log.debug("检查参数: {} = {}", property, value);

                    // 提取实际的字段名（去掉对象前缀）
                    String fieldName = extractFieldName(property);
                    
                    ParameterEncryptInfo encryptInfo = matchParameterToTableField(
                            fieldName, (String) value, sqlAnalysis);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(property);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加加密参数: {}", property);
                    }
                }
            } catch (Exception e) {
                log.debug("获取参数值失败: {}", property, e);
            }
        }

        // 方法2：直接遍历 Map 中的参数
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();

            if (paramValue instanceof String) {
                // 跳过已经通过 ParameterMapping 处理的参数
                boolean alreadyProcessed = parameterMappings.stream()
                        .anyMatch(mapping -> paramName.equals(mapping.getProperty()));

                if (!alreadyProcessed) {
                    ParameterEncryptInfo encryptInfo = matchParameterToTableField(
                            paramName, (String) paramValue, sqlAnalysis);
                    if (encryptInfo != null) {
                        encryptInfo.setParameterKey(paramName);
                        encryptInfo.setParameterMap(paramMap);
                        encryptInfo.setMetaObject(metaObject);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加额外加密参数: {}", paramName);
                    }
                }
            }
        }
    }

    /**
     * 分析对象参数
     */
    private void analyzeObjectParameters(Object parameterObject,
                                         List<ParameterMapping> parameterMappings,
                                         SqlAnalysisResult sqlAnalysis,
                                         List<ParameterEncryptInfo> encryptInfos) {

        log.debug("分析对象参数: {}", parameterObject.getClass().getSimpleName());

        // 通过 ParameterMapping 获取需要处理的属性
        for (ParameterMapping mapping : parameterMappings) {
            String property = mapping.getProperty();

            try {
                Object value = getPropertyValue(parameterObject, property);

                if (value instanceof String) {
                    log.debug("检查对象属性: {} = {}", property, value);

                    ParameterEncryptInfo encryptInfo = matchParameterToTableField(
                            property, (String) value, sqlAnalysis);
                    if (encryptInfo != null) {
                        encryptInfo.setTargetObject(parameterObject);
                        encryptInfo.setPropertyName(property);
                        encryptInfos.add(encryptInfo);
                        log.debug("添加对象加密属性: {}", property);
                    }
                }
            } catch (Exception e) {
                log.debug("获取对象属性值失败: {}", property, e);
            }
        }
    }

    /**
     * 匹配参数到表字段
     */
    private ParameterEncryptInfo matchParameterToTableField(String paramName, String paramValue,
                                                            SqlAnalysisResult sqlAnalysis) {
        TableContainer tableContainer = SpringContextUtil.getBean(TableContainer.class);
        // 清理参数名
        String cleanParamName = cleanParameterName(paramName);

        // 遍历所有表，检查是否有匹配的加密字段
        for (TableInfo tableInfo : sqlAnalysis.getTables()) {
            String tableName = tableInfo.getTableName();

            // 直接用参数名作为字段名检查
            if (tableContainer.isEncrypt(tableName, cleanParamName)) {
                return createEncryptInfo(tableName, cleanParamName, paramValue);
            }

            // 尝试驼峰转下划线
            String underscoreName = camelToUnderscore(cleanParamName);
            if (tableContainer.isEncrypt(tableName, underscoreName)) {
                return createEncryptInfo(tableName, underscoreName, paramValue);
            }

            // 尝试下划线转驼峰
            String camelName = underscoreToCamel(cleanParamName);
            if (tableContainer.isEncrypt(tableName, camelName)) {
                return createEncryptInfo(tableName, camelName, paramValue);
            }
        }

        // 从 SQL 条件中匹配
        for (FieldCondition condition : sqlAnalysis.getConditions()) {
            String columnName = condition.getColumnName();

            if (cleanParamName.equalsIgnoreCase(columnName) ||
                    camelToUnderscore(cleanParamName).equalsIgnoreCase(columnName) ||
                    underscoreToCamel(cleanParamName).equalsIgnoreCase(columnName)) {

                // 找到匹配的字段，检查哪个表包含这个加密字段
                for (TableInfo tableInfo : sqlAnalysis.getTables()) {
                    String tableName = tableInfo.getTableName();
                    if (tableContainer.isEncrypt(tableName, columnName)) {
                        return createEncryptInfo(tableName, columnName, paramValue);
                    }
                }
            }
        }

        return null;
    }

    /**
     * 执行参数加密
     */
    private void encryptParameters(List<ParameterEncryptInfo> encryptInfos) {
        for (ParameterEncryptInfo encryptInfo : encryptInfos) {
            try {
                EncryptionAlgo algo = EncryptionAlgoContainer.getAlgo(encryptInfo.getAlgoClass());
                String encryptedValue = algo.encrypt(encryptInfo.getOriginalValue());

                // 根据参数类型更新加密后的值
                if (encryptInfo.getParameterMap() != null && encryptInfo.getParameterKey() != null) {
                    // 优先使用 MetaObject 来设置嵌套属性
                    if (encryptInfo.getMetaObject() != null && encryptInfo.getMetaObject().hasSetter(encryptInfo.getParameterKey())) {
                        encryptInfo.getMetaObject().setValue(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("通过 MetaObject 更新嵌套参数: {} = {}", encryptInfo.getParameterKey(), encryptedValue);
                    } else {
                        // 回退到直接 Map 操作
                        encryptInfo.getParameterMap().put(encryptInfo.getParameterKey(), encryptedValue);
                        log.debug("更新 Map 参数: {} = {}", encryptInfo.getParameterKey(), encryptedValue);
                    }

                } else if (encryptInfo.getTargetObject() != null && encryptInfo.getPropertyName() != null) {
                    // 对象属性
                    setPropertyValue(encryptInfo.getTargetObject(), encryptInfo.getPropertyName(), encryptedValue);
                    log.debug("更新对象属性: {} = {}", encryptInfo.getPropertyName(), encryptedValue);
                }

                log.debug("字段 {}.{} 加密完成: {} -> {}",
                        encryptInfo.getTableName(), encryptInfo.getFieldName(),
                        encryptInfo.getOriginalValue(), encryptedValue);

            } catch (Exception e) {
                log.error("加密参数失败: {}.{}", encryptInfo.getTableName(), encryptInfo.getFieldName(), e);
            }
        }
    }

    /**
     * 获取对象的属性值
     */
    private Object getPropertyValue(Object obj, String propertyName) throws Exception {
        // 尝试通过 getter 方法获取
        String getterName = "get" + StringUtils.capitalize(propertyName);
        try {
            java.lang.reflect.Method getter = obj.getClass().getMethod(getterName);
            return getter.invoke(obj);
        } catch (NoSuchMethodException e) {
            // 尝试 boolean 类型的 is 方法
            String isGetterName = "is" + StringUtils.capitalize(propertyName);
            try {
                java.lang.reflect.Method isGetter = obj.getClass().getMethod(isGetterName);
                return isGetter.invoke(obj);
            } catch (NoSuchMethodException e2) {
                // 直接通过字段访问
                Field field = findField(obj.getClass(), propertyName);
                if (field != null) {
                    field.setAccessible(true);
                    return field.get(obj);
                }
                throw new Exception("无法获取属性: " + propertyName);
            }
        }
    }

    /**
     * 设置对象的属性值
     */
    private void setPropertyValue(Object obj, String propertyName, Object value) throws Exception {
        // 尝试通过 setter 方法设置
        String setterName = "set" + StringUtils.capitalize(propertyName);
        try {
            java.lang.reflect.Method setter = obj.getClass().getMethod(setterName, String.class);
            setter.invoke(obj, value);
        } catch (NoSuchMethodException e) {
            // 直接通过字段设置
            Field field = findField(obj.getClass(), propertyName);
            if (field != null) {
                field.setAccessible(true);
                field.set(obj, value);
            } else {
                throw new Exception("无法设置属性: " + propertyName);
            }
        }
    }

    /**
     * 递归查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 清理参数名
     */
    private String cleanParameterName(String paramName) {
        if (paramName == null) {
            return null;
        }

        // 移除常见的前缀
        String cleaned = paramName;
        if (cleaned.startsWith("param.")) {
            cleaned = cleaned.substring(6);
        }
        if (cleaned.startsWith("arg.")) {
            cleaned = cleaned.substring(4);
        }

        return cleaned;
    }

    /**
     * 从嵌套属性路径中提取字段名
     * 例如：userParam.phoneNo -> phoneNo
     */
    private String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return propertyPath;
        }
        
        // 如果包含点号，取最后一部分作为字段名
        int lastDotIndex = propertyPath.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }
        
        return propertyPath;
    }

    /**
     * 驼峰转下划线
     */
    private String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 下划线转驼峰
     */
    private String underscoreToCamel(String underscore) {
        if (underscore == null || underscore.isEmpty()) {
            return underscore;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (char c : underscore.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * 创建加密信息对象
     */
    private ParameterEncryptInfo createEncryptInfo(String tableName, String fieldName, String value) {
        TableContainer tableContainer = SpringContextUtil.getBean(TableContainer.class);
        Class<? extends EncryptionAlgo> algoClass = tableContainer.getAlgo(tableName, fieldName);

        ParameterEncryptInfo encryptInfo = new ParameterEncryptInfo();
        encryptInfo.setTableName(tableName);
        encryptInfo.setFieldName(fieldName);
        encryptInfo.setOriginalValue(value);
        encryptInfo.setAlgoClass(algoClass);

        return encryptInfo;
    }

    /**
     * SQL 分析结果
     */
    private static class SqlAnalysisResult {
        private final List<TableInfo> tables = new ArrayList<>();
        private final List<FieldCondition> conditions = new ArrayList<>();

        public void addTable(TableInfo tableInfo) {
            tables.add(tableInfo);
        }

        public void addCondition(FieldCondition condition) {
            conditions.add(condition);
        }

        public List<TableInfo> getTables() {
            return tables;
        }

        public List<FieldCondition> getConditions() {
            return conditions;
        }
    }

    /**
     * 表信息
     */
    private static class TableInfo {
        private final String tableName;
        private final String alias;

        public TableInfo(String tableName, String alias) {
            this.tableName = tableName;
            this.alias = alias;
        }

        public String getTableName() {
            return tableName;
        }

        public String getAlias() {
            if (alias == null) {
                return tableName;
            }
            return alias;
        }
    }

    /**
     * 字段条件信息
     */
    private static class FieldCondition {
        private final String tableAlias;
        private final String columnName;

        public FieldCondition(String tableAlias, String columnName) {
            this.tableAlias = tableAlias;
            this.columnName = columnName;
        }

        public String getTableAlias() {
            return tableAlias;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    /**
     * 参数加密信息
     */
    private static class ParameterEncryptInfo {
        private String tableName;
        private String fieldName;
        private String originalValue;
        private Class<? extends EncryptionAlgo> algoClass;

        // Map 参数相关
        private Map<String, Object> parameterMap;
        private String parameterKey;
        private MetaObject metaObject;

        // 对象参数相关
        private Object targetObject;
        private String propertyName;

        // Getters and Setters
        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getOriginalValue() {
            return originalValue;
        }

        public void setOriginalValue(String originalValue) {
            this.originalValue = originalValue;
        }

        public Class<? extends EncryptionAlgo> getAlgoClass() {
            return algoClass;
        }

        public void setAlgoClass(Class<? extends EncryptionAlgo> algoClass) {
            this.algoClass = algoClass;
        }

        public Map<String, Object> getParameterMap() {
            return parameterMap;
        }

        public void setParameterMap(Map<String, Object> parameterMap) {
            this.parameterMap = parameterMap;
        }

        public String getParameterKey() {
            return parameterKey;
        }

        public void setParameterKey(String parameterKey) {
            this.parameterKey = parameterKey;
        }

        public Object getTargetObject() {
            return targetObject;
        }

        public void setTargetObject(Object targetObject) {
            this.targetObject = targetObject;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public MetaObject getMetaObject() {
            return metaObject;
        }

        public void setMetaObject(MetaObject metaObject) {
            this.metaObject = metaObject;
        }
    }
}