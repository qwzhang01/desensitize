package io.github.qwzhang01.desensitize.shield;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import io.github.qwzhang01.desensitize.kit.SqlUtil;

public class TestUpdateSql {
    public static void main(String[] args) {
        // 测试UPDATE语句解析
        String updateSql = "UPDATE user SET `name`=?, phoneNo=?, gender=?, idNo=? WHERE id=?";

        SqlAnalysisInfo analysis = SqlUtil.analyzeSql(updateSql);

        System.out.println("表信息:");
        for (SqlAnalysisInfo.TableInfo table : analysis.getTables()) {
            System.out.println("  表名: " + table.tableName() + ", 别名: " + table.alias());
        }

        System.out.println("\nSET字段:");
        for (SqlAnalysisInfo.FieldCondition field : analysis.getSetFields()) {
            System.out.println("  字段: " + field.columnName());
        }

        System.out.println("\nWHERE条件字段:");
        for (SqlAnalysisInfo.FieldCondition field : analysis.getConditions()) {
            System.out.println("  字段: " + field.columnName());
        }

        System.out.println("\n所有字段（按参数顺序）:");
        for (int i = 0; i < analysis.getAllFields().size(); i++) {
            SqlAnalysisInfo.FieldCondition field = analysis.getAllFields().get(i);
            System.out.println("  参数" + i + ": " + field.columnName());
        }
    }
}