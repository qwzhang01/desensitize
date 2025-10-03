package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SqlUtil 测试类
 * 测试各种复杂 SQL 语句的解析功能
 */
public class SqlUtilTest {

    @Test
    @DisplayName("测试简单 SELECT 语句解析")
    public void testSimpleSelect() {
        String sql = "SELECT id, name, phone FROM user WHERE phone = ? AND status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(2, result.getConditions().size());
        assertEquals(2, result.getParameterMappings().size());

        // 验证参数映射
        assertEquals("phone", result.getParameterMappings().get(0).fieldName());
        assertEquals("status", result.getParameterMappings().get(1).fieldName());
    }

    @Test
    @DisplayName("测试带别名的 SELECT 语句解析")
    public void testSelectWithAlias() {
        String sql = "SELECT u.id, u.name, u.phone FROM user u WHERE u.phone = ? AND u.status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("u", result.getTables().get(0).alias());
        assertEquals(2, result.getConditions().size());

        // 验证字段的表别名
        assertEquals("u", result.getConditions().get(0).tableAlias());
        assertEquals("phone", result.getConditions().get(0).columnName());
    }

    @Test
    @DisplayName("测试 JOIN 查询解析")
    public void testJoinQuery() {
        String sql = "SELECT u.name, p.title FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id " +
                "WHERE u.phone = ? AND p.status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(2, result.getTables().size());

        // 验证主表
        TableInfo mainTable = result.getTables().get(0);
        assertEquals("user", mainTable.tableName());
        assertEquals("u", mainTable.alias());
        assertEquals(TableType.MAIN, mainTable.tableType());

        // 验证 JOIN 表
        TableInfo joinTable = result.getTables().get(1);
        assertEquals("profile", joinTable.tableName());
        assertEquals("p", joinTable.alias());
        assertEquals(TableType.JOIN, joinTable.tableType());

        // 验证条件字段
        assertEquals(2, result.getConditions().size());
        assertEquals("u", result.getConditions().get(0).tableAlias());
        assertEquals("phone", result.getConditions().get(0).columnName());
        assertEquals("p", result.getConditions().get(1).tableAlias());
        assertEquals("status", result.getConditions().get(1).columnName());
    }

    @Test
    @DisplayName("测试复杂 JOIN 查询解析")
    public void testComplexJoinQuery() {
        String sql = "SELECT u.name, p.title, d.name as dept_name " +
                "FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id " +
                "INNER JOIN department d ON u.dept_id = d.id " +
                "WHERE u.phone = ? AND p.status = ? AND d.code = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(3, result.getTables().size());
        assertEquals(3, result.getConditions().size());

        // 验证表信息
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("profile", result.getTables().get(1).tableName());
        assertEquals("department", result.getTables().get(2).tableName());
    }

    @Test
    @DisplayName("测试 INSERT 语句解析")
    public void testInsertStatement() {
        String sql = "INSERT INTO user (name, phone, email, status) VALUES (?, ?, ?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.INSERT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(4, result.getInsertFields().size());
        assertEquals(4, result.getParameterMappings().size());

        // 验证插入字段
        assertEquals("name", result.getInsertFields().get(0).columnName());
        assertEquals("phone", result.getInsertFields().get(1).columnName());
        assertEquals("email", result.getInsertFields().get(2).columnName());
        assertEquals("status", result.getInsertFields().get(3).columnName());

        // 验证参数映射
        for (int i = 0; i < 4; i++) {
            assertEquals(FieldType.INSERT, result.getParameterMappings().get(i).fieldType());
        }
    }

    @Test
    @DisplayName("测试 UPDATE 语句解析")
    public void testUpdateStatement() {
        String sql = "UPDATE user SET name = ?, phone = ?, status = ? WHERE id = ? AND dept_id = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.UPDATE, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(3, result.getSetFields().size());
        assertEquals(2, result.getConditions().size());
        assertEquals(5, result.getParameterMappings().size());

        // 验证 SET 字段
        assertEquals("name", result.getSetFields().get(0).columnName());
        assertEquals("phone", result.getSetFields().get(1).columnName());
        assertEquals("status", result.getSetFields().get(2).columnName());

        // 验证 WHERE 条件
        assertEquals("id", result.getConditions().get(0).columnName());
        assertEquals("dept_id", result.getConditions().get(1).columnName());

        // 验证参数映射顺序（SET 字段在前，WHERE 条件在后）
        assertEquals(FieldType.UPDATE_SET, result.getParameterMappings().get(0).fieldType());
        assertEquals(FieldType.UPDATE_SET, result.getParameterMappings().get(1).fieldType());
        assertEquals(FieldType.UPDATE_SET, result.getParameterMappings().get(2).fieldType());
        assertEquals(FieldType.CONDITION, result.getParameterMappings().get(3).fieldType());
        assertEquals(FieldType.CONDITION, result.getParameterMappings().get(4).fieldType());
    }

    @Test
    @DisplayName("测试带别名的 UPDATE 语句解析")
    public void testUpdateWithAlias() {
        String sql = "UPDATE user u SET u.name = ?, u.phone = ? WHERE u.id = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.UPDATE, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("u", result.getTables().get(0).alias());
        assertEquals(2, result.getSetFields().size());
        assertEquals(1, result.getConditions().size());
    }

    @Test
    @DisplayName("测试 DELETE 语句解析")
    public void testDeleteStatement() {
        String sql = "DELETE FROM user WHERE phone = ? AND status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.DELETE, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(2, result.getConditions().size());
        assertEquals(2, result.getParameterMappings().size());

        // 验证条件字段
        assertEquals("phone", result.getConditions().get(0).columnName());
        assertEquals("status", result.getConditions().get(1).columnName());
    }

    @Test
    @DisplayName("测试带别名的 DELETE 语句解析")
    public void testDeleteWithAlias() {
        String sql = "DELETE FROM user u WHERE u.phone = ? AND u.status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.DELETE, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("u", result.getTables().get(0).alias());
        assertEquals(2, result.getConditions().size());

        // 验证字段的表别名
        assertEquals("u", result.getConditions().get(0).tableAlias());
        assertEquals("u", result.getConditions().get(1).tableAlias());
    }

    @Test
    @DisplayName("测试带反引号的表名和字段名")
    public void testWithBackticks() {
        String sql = "SELECT `user`.`name`, `user`.`phone` FROM `user` WHERE `user`.`phone` = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(1, result.getConditions().size());
        assertEquals("phone", result.getConditions().get(0).columnName());
    }

    @Test
    @DisplayName("测试复杂的 WHERE 条件")
    public void testComplexWhereConditions() {
        String sql = "SELECT * FROM user u WHERE u.isDel = 1 AND u.phone LIKE ? AND u.status IN (?, ?) AND u.age > ? AND u.name IS NOT NULL AND u.id between(?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        // 注意：IS NOT NULL 不会产生参数占位符，所以只有3个条件字段
        assertEquals(3, result.getConditions().size());
        assertEquals(3, result.getParameterMappings().size());
    }

    @Test
    @DisplayName("测试空 SQL 处理")
    public void testEmptySQL() {
        SqlAnalysisInfo result1 = SqlUtil.analyzeSql("");
        SqlAnalysisInfo result2 = SqlUtil.analyzeSql(null);
        SqlAnalysisInfo result3 = SqlUtil.analyzeSql("   ");

        assertTrue(result1.getTables().isEmpty());
        assertTrue(result2.getTables().isEmpty());
        assertTrue(result3.getTables().isEmpty());
    }

    @Test
    @DisplayName("测试 SQL 注释处理")
    public void testSQLWithComments() {
        String sql = "SELECT u.name, u.phone /* 用户信息 */ FROM user u -- 用户表 \n WHERE u.phone = ? /* 手机号条件 */";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals(1, result.getConditions().size());
    }

    @Test
    @DisplayName("测试参数映射的表名解析")
    public void testParameterMappingTableName() {
        String sql = "SELECT u.name FROM user u LEFT JOIN profile p ON u.id = p.user_id WHERE u.phone = ? AND p.status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(2, result.getParameterMappings().size());

        // 第一个参数应该映射到 user 表
        ParameterFieldMapping mapping1 = result.getParameterMappings().get(0);
        assertEquals("user", mapping1.tableName());
        assertEquals("phone", mapping1.fieldName());
        assertEquals("u", mapping1.tableAlias());

        // 第二个参数应该映射到 profile 表
        ParameterFieldMapping mapping2 = result.getParameterMappings().get(1);
        assertEquals("profile", mapping2.tableName());
        assertEquals("status", mapping2.fieldName());
        assertEquals("p", mapping2.tableAlias());
    }
}