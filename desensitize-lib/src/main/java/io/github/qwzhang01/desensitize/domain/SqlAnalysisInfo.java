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

    /**
     * 表信息
     */
    public record TableInfo(String tableName, String alias) {

        @Override
        public String alias() {
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
