package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * JOIN ON 条件解析测试
 */
public class JoinOnTest {
    @Test
    @DisplayName("测试简单 join 语句解析")
    public void testJoin() {
        // 测试简单的 JOIN ON 条件
        String sql1 = "SELECT u.name, p.title FROM users u " +
                "LEFT JOIN posts p ON u.id = p.user_id " +
                "WHERE u.status = ?";

        System.out.println("测试 SQL 1: " + sql1);
        SqlAnalysisInfo result1 = SqlUtil.analyzeSql(sql1);
        printAnalysisResult(result1);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试复杂的 JOIN ON 条件
        String sql2 = "SELECT u.name, p.title, c.content FROM users u " +
                "LEFT JOIN posts p ON u.id = p.user_id AND p.status = ? " +
                "INNER JOIN comments c ON p.id = c.post_id AND c.created_at > ? " +
                "WHERE u.active = ? AND p.published = ?";

        System.out.println("测试 SQL 2: " + sql2);
        SqlAnalysisInfo result2 = SqlUtil.analyzeSql(sql2);
        printAnalysisResult(result2);

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 测试带 IN 条件的 JOIN ON
        String sql3 = "SELECT u.name, r.role_name FROM users u " +
                "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                "LEFT JOIN roles r ON ur.role_id = r.id AND r.status IN (?, ?) " +
                "WHERE u.department_id = ?";

        System.out.println("测试 SQL 3: " + sql3);
        SqlAnalysisInfo result3 = SqlUtil.analyzeSql(sql3);
        printAnalysisResult(result3);
    }

    private void printAnalysisResult(SqlAnalysisInfo result) {
        System.out.println("SQL 类型: " + result.getSqlType());

        System.out.println("表信息:");
        result.getTables().forEach(table ->
                System.out.println("  - " + table.tableName() +
                        (table.alias() != null ? " (别名: " + table.alias() + ")" : "") +
                        " [" + table.tableType() + "]"));

        System.out.println("条件字段:");
        result.getConditions().forEach(condition ->
                System.out.println("  - " +
                        (condition.tableAlias() != null ? condition.tableAlias() + "." : "") +
                        condition.columnName() +
                        " [" + condition.fieldType() + ", " + condition.operatorType() +
                        ", 参数数量: " + condition.getEffectiveParamCount() + "]"));

        System.out.println("参数映射:");
        result.getParameterMappings().forEach(mapping ->
                System.out.println("  参数[" + mapping.parameterIndex() + "] -> " +
                        mapping.tableName() + "." + mapping.fieldName() +
                        " [" + mapping.fieldType() + "]"));
    }
}