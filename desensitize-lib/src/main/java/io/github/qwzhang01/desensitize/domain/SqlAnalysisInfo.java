package io.github.qwzhang01.desensitize.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 解析结果
 *
 * @author avinzhang
 */
public class SqlAnalysisInfo {

    // 表信息
    private final List<TableInfo> tables = new ArrayList<>();
    // WHERE 条件字段
    private final List<FieldCondition> conditions = new ArrayList<>();
    // UPDATE 语句的 SET 字段
    private final List<FieldCondition> setFields = new ArrayList<>();
    // INSERT 语句的字段
    private final List<FieldCondition> insertFields = new ArrayList<>();
    // SELECT 语句的字段
    private final List<FieldCondition> selectFields = new ArrayList<>();
    // 参数占位符与字段的映射关系（按顺序）
    private final List<ParameterFieldMapping> parameterMappings = new ArrayList<>();
    // 表别名映射
    private final Map<String, String> aliasToTableMap = new HashMap<>();
    // SQL 语句类型
    private SqlType sqlType;

    // ========== 基本操作方法 ==========

    public SqlType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SqlType sqlType) {
        this.sqlType = sqlType;
    }

    public void addTable(TableInfo tableInfo) {
        tables.add(tableInfo);
        // 维护别名映射
        if (tableInfo.alias() != null && !tableInfo.alias().trim().isEmpty()) {
            aliasToTableMap.put(tableInfo.alias(), tableInfo.tableName());
        }
    }

    public void addCondition(FieldCondition condition) {
        conditions.add(condition);
    }

    public void addSetField(FieldCondition setField) {
        setFields.add(setField);
    }

    public void addInsertField(FieldCondition insertField) {
        insertFields.add(insertField);
    }

    public void addSelectField(FieldCondition selectField) {
        selectFields.add(selectField);
    }

    public void addParameterMapping(ParameterFieldMapping mapping) {
        parameterMappings.add(mapping);
    }

    // ========== 获取方法 ==========

    public List<TableInfo> getTables() {
        return tables;
    }

    public List<FieldCondition> getConditions() {
        return conditions;
    }

    public List<FieldCondition> getSetFields() {
        return setFields;
    }

    public List<FieldCondition> getInsertFields() {
        return insertFields;
    }

    public List<FieldCondition> getSelectFields() {
        return selectFields;
    }

    public List<ParameterFieldMapping> getParameterMappings() {
        return parameterMappings;
    }

    public Map<String, String> getAliasToTableMap() {
        return aliasToTableMap;
    }

    /**
     * 获取所有字段（按SQL中出现的顺序）
     * 对于不同类型的SQL，字段顺序不同：
     * - INSERT: insertFields
     * - UPDATE: setFields + conditions
     * - SELECT/DELETE: conditions
     */
    public List<FieldCondition> getAllFields() {
        List<FieldCondition> allFields = new ArrayList<>();

        switch (sqlType) {
            case INSERT:
                allFields.addAll(insertFields);
                break;
            case UPDATE:
                allFields.addAll(setFields);
                allFields.addAll(conditions);
                break;
            case SELECT:
            case DELETE:
                allFields.addAll(conditions);
                break;
        }

        return allFields;
    }

    /**
     * 根据别名获取真实表名
     */
    public String getRealTableName(String aliasOrTableName) {
        return aliasToTableMap.getOrDefault(aliasOrTableName, aliasOrTableName);
    }

    // ========== 内部类和枚举 ==========

    /**
     * SQL 语句类型
     */
    public enum SqlType {
        SELECT, INSERT, UPDATE, DELETE
    }

    /**
     * 表类型
     */
    public enum TableType {
        MAIN,      // 主表
        JOIN,      // JOIN 表
        SUBQUERY   // 子查询表
    }

    /**
     * 字段类型
     */
    public enum FieldType {
        SELECT,     // SELECT 字段
        INSERT,     // INSERT 字段
        UPDATE_SET, // UPDATE SET 字段
        CONDITION   // WHERE/HAVING 条件字段
    }

    /**
     * 表信息
     */
    public static record TableInfo(String tableName, String alias, TableType tableType) {

        public TableInfo(String tableName, String alias) {
            this(tableName, alias, TableType.MAIN);
        }

        public String getEffectiveAlias() {
            if (alias == null || alias.trim().isEmpty()) {
                return tableName;
            }
            return alias;
        }
    }

    /**
     * 操作符类型
     */
    public enum OperatorType {
        SINGLE_PARAM(1),    // 单参数操作符：=, !=, <, >, <=, >=, LIKE
        IN_OPERATOR(0),     // IN 操作符：参数个数动态确定
        BETWEEN_OPERATOR(2), // BETWEEN 操作符：固定2个参数
        NO_PARAM(0);        // 无参数操作符：IS NULL, IS NOT NULL

        private final int paramCount;

        OperatorType(int paramCount) {
            this.paramCount = paramCount;
        }

        public int getParamCount() {
            return paramCount;
        }
    }

    /**
     * 字段条件信息
     */
    public static class FieldCondition {
        private final String tableAlias;
        private final String columnName;
        private final FieldType fieldType;
        private final OperatorType operatorType;
        private final int actualParamCount;

        public FieldCondition(String tableAlias, String columnName) {
            this(tableAlias, columnName, FieldType.CONDITION, OperatorType.SINGLE_PARAM, 1);
        }

        public FieldCondition(String tableAlias, String columnName, FieldType fieldType) {
            this(tableAlias, columnName, fieldType, OperatorType.SINGLE_PARAM, 1);
        }

        public FieldCondition(String tableAlias, String columnName, FieldType fieldType, OperatorType operatorType, int actualParamCount) {
            this.tableAlias = tableAlias;
            this.columnName = columnName;
            this.fieldType = fieldType;
            this.operatorType = operatorType;
            this.actualParamCount = actualParamCount;
        }

        public String tableAlias() {
            return tableAlias;
        }

        public String columnName() {
            return columnName;
        }

        public FieldType fieldType() {
            return fieldType;
        }

        public OperatorType operatorType() {
            return operatorType;
        }

        public int actualParamCount() {
            return actualParamCount;
        }

        /**
         * 获取实际参数个数
         */
        public int getEffectiveParamCount() {
            if (operatorType == OperatorType.IN_OPERATOR) {
                return actualParamCount; // 对于 IN 操作符，使用实际计算的参数个数
            }
            return operatorType.getParamCount();
        }
    }

    /**
     * 参数占位符与字段的映射关系
     */
    public record ParameterFieldMapping(
            int parameterIndex,     // 参数索引（从0开始）
            String tableName,       // 表名
            String fieldName,       // 字段名
            String tableAlias,      // 表别名
            FieldType fieldType     // 字段类型
    ) {

        /**
         * 获取有效的表标识（优先使用别名）
         */
        public String getEffectiveTableIdentifier() {
            return (tableAlias != null && !tableAlias.trim().isEmpty()) ? tableAlias : tableName;
        }
    }
}
