package io.github.qwzhang01.desensitize.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 解析结果
 *
 * @author avinzhang
 */
public class SqlAnalysisInfo {
    private final List<TableInfo> tables = new ArrayList<>();
    private final List<FieldCondition> conditions = new ArrayList<>();
    private final List<FieldCondition> setFields = new ArrayList<>(); // UPDATE语句的SET字段

    public void addTable(TableInfo tableInfo) {
        tables.add(tableInfo);
    }

    public void addCondition(FieldCondition condition) {
        conditions.add(condition);
    }

    public void addSetField(FieldCondition setField) {
        setFields.add(setField);
    }

    public List<TableInfo> getTables() {
        return tables;
    }

    public List<FieldCondition> getConditions() {
        return conditions;
    }

    public List<FieldCondition> getSetFields() {
        return setFields;
    }

    /**
     * 获取所有字段（SET字段 + WHERE条件字段）
     * SET字段在前，WHERE字段在后，这样顺序与参数顺序一致
     */
    public List<FieldCondition> getAllFields() {
        List<FieldCondition> allFields = new ArrayList<>();
        allFields.addAll(setFields);
        allFields.addAll(conditions);
        return allFields;
    }

    /**
     * 表信息
     */
    public record TableInfo(String tableName, String alias) {
        
        public String getEffectiveAlias() {
            if (alias == null) {
                return tableName;
            }
            return alias;
        }
    }

    /**
     * 字段条件信息
     */
    public record FieldCondition(String tableAlias, String columnName) {
    }
}
