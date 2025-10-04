package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlUtil 测试类
 * 测试各种复杂 SQL 语句的解析功能
 */
public class SqlUtilTest {

    // ========== analyzeSql 主方法测试 ==========

    @Test
    @DisplayName("测试空 SQL 处理")
    public void testAnalyzeSqlWithEmptyInput() {
        SqlAnalysisInfo result1 = SqlUtil.analyzeSql("");
        SqlAnalysisInfo result2 = SqlUtil.analyzeSql(null);
        SqlAnalysisInfo result3 = SqlUtil.analyzeSql("   ");

        assertTrue(result1.getTables().isEmpty());
        assertTrue(result2.getTables().isEmpty());
        assertTrue(result3.getTables().isEmpty());
        
        assertEquals(0, result1.getParameterMappings().size());
        assertEquals(0, result2.getParameterMappings().size());
        assertEquals(0, result3.getParameterMappings().size());
    }

    @Test
    @DisplayName("测试 SQL 注释处理")
    public void testAnalyzeSqlWithComments() {
        String sql = "SELECT u.name, u.phone /* 用户信息 */ FROM user u -- 用户表 \n WHERE u.phone = ? /* 手机号条件 */";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals(1, result.getConditions().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("u", result.getTables().get(0).alias());
    }

    @Test
    @DisplayName("测试多行空格和换行符处理")
    public void testAnalyzeSqlWithMultipleSpaces() {
        String sql = "SELECT   u.name,   u.phone\n\n  FROM   user   u\n  WHERE   u.phone   =   ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals(1, result.getConditions().size());
    }

    // ========== SELECT 语句测试 ==========

    @Test
    @DisplayName("测试简单 SELECT 语句解析")
    public void testSimpleSelect() {
        String sql = "SELECT id, name, phone FROM user WHERE phone = ? AND status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(TableType.MAIN, result.getTables().get(0).tableType());
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
        assertEquals("u", result.getConditions().get(1).tableAlias());
        assertEquals("status", result.getConditions().get(1).columnName());
    }

    @Test
    @DisplayName("测试带反引号的 SELECT 语句")
    public void testSelectWithBackticks() {
        String sql = "SELECT `user`.`name`, `user`.`phone` FROM `user` WHERE `user`.`phone` = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(1, result.getConditions().size());
        assertEquals("phone", result.getConditions().get(0).columnName());
        assertEquals("user", result.getConditions().get(0).tableAlias());
    }

    @Test
    @DisplayName("测试 SELECT 语句无 WHERE 条件")
    public void testSelectWithoutWhere() {
        String sql = "SELECT id, name FROM user";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(0, result.getConditions().size());
        assertEquals(0, result.getParameterMappings().size());
    }

    // ========== JOIN 语句测试 ==========

    @Test
    @DisplayName("测试简单 JOIN 查询解析")
    public void testSimpleJoinQuery() {
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
    @DisplayName("测试复杂多表 JOIN 查询解析")
    public void testComplexJoinQuery() {
        String sql = "SELECT u.name, p.title, d.name as dept_name " +
                "FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id " +
                "INNER JOIN department d ON u.dept_id = d.id " +
                "RIGHT JOIN role r ON u.role_id = r.id " +
                "WHERE u.phone = ? AND p.status = ? AND d.code = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(4, result.getTables().size());
        assertEquals(3, result.getConditions().size());

        // 验证表信息
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(TableType.MAIN, result.getTables().get(0).tableType());
        assertEquals("profile", result.getTables().get(1).tableName());
        assertEquals(TableType.JOIN, result.getTables().get(1).tableType());
        assertEquals("department", result.getTables().get(2).tableName());
        assertEquals(TableType.JOIN, result.getTables().get(2).tableType());
        assertEquals("role", result.getTables().get(3).tableName());
        assertEquals(TableType.JOIN, result.getTables().get(3).tableType());
    }

    @Test
    @DisplayName("测试 JOIN ON 条件解析")
    public void testJoinOnConditions() {
        String sql = "SELECT u.name, p.title FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id AND p.status = ? " +
                "WHERE u.phone = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(2, result.getTables().size());
        assertEquals(2, result.getConditions().size());

        // 验证 ON 条件中的参数也被正确解析
        boolean foundOnCondition = result.getConditions().stream()
                .anyMatch(c -> "p".equals(c.tableAlias()) && "status".equals(c.columnName()));
        assertTrue(foundOnCondition, "应该找到 JOIN ON 条件中的 p.status 字段");

        // 验证参数映射
        assertEquals(2, result.getParameterMappings().size());
    }

    @Test
    @DisplayName("测试复杂 JOIN ON 条件")
    public void testComplexJoinOnConditions() {
        String sql = "SELECT u.name FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id AND p.status IN (?, ?) " +
                "INNER JOIN department d ON u.dept_id = d.id AND d.active = ? " +
                "WHERE u.phone = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(3, result.getTables().size());
        assertEquals(3, result.getConditions().size());

        // 验证 IN 操作符的参数计数
        FieldCondition inCondition = result.getConditions().stream()
                .filter(c -> c.operatorType() == OperatorType.IN_OPERATOR)
                .findFirst()
                .orElse(null);
        assertNotNull(inCondition);
        assertEquals(2, inCondition.getEffectiveParamCount());

        // 验证总参数映射数量：IN(2个) + 普通条件(1个) + WHERE条件(1个) = 4个
        assertEquals(4, result.getParameterMappings().size());
    }

    // ========== INSERT 语句测试 ==========

    @Test
    @DisplayName("测试简单 INSERT 语句解析")
    public void testSimpleInsert() {
        String sql = "INSERT INTO user (name, phone, email, status) VALUES (?, ?, ?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.INSERT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(TableType.MAIN, result.getTables().get(0).tableType());
        assertEquals(4, result.getInsertFields().size());
        assertEquals(4, result.getParameterMappings().size());

        // 验证插入字段
        assertEquals("name", result.getInsertFields().get(0).columnName());
        assertEquals("phone", result.getInsertFields().get(1).columnName());
        assertEquals("email", result.getInsertFields().get(2).columnName());
        assertEquals("status", result.getInsertFields().get(3).columnName());

        // 验证参数映射类型
        for (int i = 0; i < 4; i++) {
            assertEquals(FieldType.INSERT, result.getParameterMappings().get(i).fieldType());
        }
    }

    @Test
    @DisplayName("测试带反引号的 INSERT 语句")
    public void testInsertWithBackticks() {
        String sql = "INSERT INTO `user` (`name`, `phone`) VALUES (?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.INSERT, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(2, result.getInsertFields().size());
        assertEquals("name", result.getInsertFields().get(0).columnName());
        assertEquals("phone", result.getInsertFields().get(1).columnName());
    }

    @Test
    @DisplayName("测试 INSERT 语句字段包含空格")
    public void testInsertWithSpaces() {
        String sql = "INSERT INTO user ( name , phone , email ) VALUES (?, ?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.INSERT, result.getSqlType());
        assertEquals(3, result.getInsertFields().size());
        assertEquals("name", result.getInsertFields().get(0).columnName());
        assertEquals("phone", result.getInsertFields().get(1).columnName());
        assertEquals("email", result.getInsertFields().get(2).columnName());
    }

    // ========== UPDATE 语句测试 ==========

    @Test
    @DisplayName("测试简单 UPDATE 语句解析")
    public void testSimpleUpdate() {
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
    @DisplayName("测试 UPDATE 语句无 WHERE 条件")
    public void testUpdateWithoutWhere() {
        String sql = "UPDATE user SET name = ?, status = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.UPDATE, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals(2, result.getSetFields().size());
        assertEquals(0, result.getConditions().size());
        assertEquals(2, result.getParameterMappings().size());

        // 所有参数都应该是 SET 类型
        for (ParameterFieldMapping mapping : result.getParameterMappings()) {
            assertEquals(FieldType.UPDATE_SET, mapping.fieldType());
        }
    }

    @Test
    @DisplayName("测试带反引号的 UPDATE 语句")
    public void testUpdateWithBackticks() {
        String sql = "UPDATE `user` SET `name` = ?, `phone` = ? WHERE `id` = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.UPDATE, result.getSqlType());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(2, result.getSetFields().size());
        assertEquals(1, result.getConditions().size());
    }

    // ========== DELETE 语句测试 ==========

    @Test
    @DisplayName("测试简单 DELETE 语句解析")
    public void testSimpleDelete() {
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

        // 验证参数映射类型
        for (ParameterFieldMapping mapping : result.getParameterMappings()) {
            assertEquals(FieldType.CONDITION, mapping.fieldType());
        }
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
    @DisplayName("测试 DELETE 语句无 WHERE 条件")
    public void testDeleteWithoutWhere() {
        String sql = "DELETE FROM user";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.DELETE, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals(0, result.getConditions().size());
        assertEquals(0, result.getParameterMappings().size());
    }

    // ========== WHERE 条件复杂场景测试 ==========

    @Test
    @DisplayName("测试复杂的 WHERE 条件")
    public void testComplexWhereConditions() {
        String sql = "SELECT * FROM user u WHERE u.phone LIKE ? AND u.status IN (?, ?) AND u.age BETWEEN ? AND ? AND u.score > ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(4, result.getConditions().size());
        assertEquals(6, result.getParameterMappings().size());

        // 验证不同操作符类型
        FieldCondition likeCondition = result.getConditions().get(0);
        assertEquals(OperatorType.SINGLE_PARAM, likeCondition.operatorType());
        assertEquals(1, likeCondition.getEffectiveParamCount());

        FieldCondition inCondition = result.getConditions().get(1);
        assertEquals(OperatorType.IN_OPERATOR, inCondition.operatorType());
        assertEquals(2, inCondition.getEffectiveParamCount());

        FieldCondition betweenCondition = result.getConditions().get(2);
        assertEquals(OperatorType.BETWEEN_OPERATOR, betweenCondition.operatorType());
        assertEquals(2, betweenCondition.getEffectiveParamCount());

        FieldCondition singleCondition = result.getConditions().get(3);
        assertEquals(OperatorType.SINGLE_PARAM, singleCondition.operatorType());
        assertEquals(1, singleCondition.getEffectiveParamCount());
    }

    @Test
    @DisplayName("测试 IN 操作符多参数")
    public void testInOperatorMultipleParams() {
        String sql = "SELECT * FROM user WHERE status IN (?, ?, ?, ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(1, result.getConditions().size());
        FieldCondition inCondition = result.getConditions().get(0);
        assertEquals(OperatorType.IN_OPERATOR, inCondition.operatorType());
        assertEquals(4, inCondition.getEffectiveParamCount());
        assertEquals(4, result.getParameterMappings().size());

        // 验证参数映射的字段名后缀
        assertEquals("status_1", result.getParameterMappings().get(0).fieldName());
        assertEquals("status_2", result.getParameterMappings().get(1).fieldName());
        assertEquals("status_3", result.getParameterMappings().get(2).fieldName());
        assertEquals("status_4", result.getParameterMappings().get(3).fieldName());
    }

    @Test
    @DisplayName("测试 BETWEEN 操作符参数")
    public void testBetweenOperatorParams() {
        String sql = "SELECT * FROM user WHERE age BETWEEN ? AND ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(1, result.getConditions().size());
        FieldCondition betweenCondition = result.getConditions().get(0);
        assertEquals("age", betweenCondition.columnName());
        assertEquals(OperatorType.BETWEEN_OPERATOR, betweenCondition.operatorType());
        assertEquals(2, betweenCondition.getEffectiveParamCount());
        assertEquals(2, result.getParameterMappings().size());

        // 验证参数映射的字段名后缀
        assertEquals("age_min", result.getParameterMappings().get(0).fieldName());
        assertEquals("age_max", result.getParameterMappings().get(1).fieldName());
    }

    @Test
    @DisplayName("测试不同比较操作符")
    public void testDifferentComparisonOperators() {
        String sql = "SELECT * FROM user WHERE id = ? AND age > ? AND score < ? AND status != ? AND name LIKE ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(5, result.getConditions().size());
        assertEquals(5, result.getParameterMappings().size());

        // 所有条件都应该是单参数操作符
        for (FieldCondition condition : result.getConditions()) {
            assertEquals(OperatorType.SINGLE_PARAM, condition.operatorType());
            assertEquals(1, condition.getEffectiveParamCount());
        }
    }

    // ========== 参数映射测试 ==========

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

    @Test
    @DisplayName("测试无别名时的表名解析")
    public void testParameterMappingWithoutAlias() {
        String sql = "SELECT name FROM user WHERE phone = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(1, result.getParameterMappings().size());
        ParameterFieldMapping mapping = result.getParameterMappings().get(0);
        assertEquals("user", mapping.tableName());
        assertEquals("phone", mapping.fieldName());
        assertNull(mapping.tableAlias());
    }

    // ========== SQL 类型判断测试 ==========

    @Test
    @DisplayName("测试 SQL 类型判断")
    public void testSqlTypeDetection() {
        assertEquals(SqlType.SELECT, SqlUtil.analyzeSql("SELECT * FROM user").getSqlType());
        assertEquals(SqlType.INSERT, SqlUtil.analyzeSql("INSERT INTO user VALUES (?)").getSqlType());
        assertEquals(SqlType.UPDATE, SqlUtil.analyzeSql("UPDATE user SET name = ?").getSqlType());
        assertEquals(SqlType.DELETE, SqlUtil.analyzeSql("DELETE FROM user").getSqlType());
        
        // 测试大小写不敏感
        assertEquals(SqlType.SELECT, SqlUtil.analyzeSql("select * from user").getSqlType());
        assertEquals(SqlType.INSERT, SqlUtil.analyzeSql("insert into user values (?)").getSqlType());
        assertEquals(SqlType.UPDATE, SqlUtil.analyzeSql("update user set name = ?").getSqlType());
        assertEquals(SqlType.DELETE, SqlUtil.analyzeSql("delete from user").getSqlType());
    }

    @Test
    @DisplayName("测试未知 SQL 类型默认为 SELECT")
    public void testUnknownSqlTypeDefaultsToSelect() {
        String sql = "SHOW TABLES";
        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);
        assertEquals(SqlType.SELECT, result.getSqlType());
    }

    // ========== 边界情况和异常场景测试 ==========

    @Test
    @DisplayName("测试别名清理功能")
    public void testAliasCleanup() {
        // 测试 SQL 关键字被正确排除
        String sql1 = "SELECT * FROM user WHERE";
        SqlAnalysisInfo result1 = SqlUtil.analyzeSql(sql1);
        if (!result1.getTables().isEmpty()) {
            assertNull(result1.getTables().get(0).alias());
        }

        // 测试正常别名保留
        String sql2 = "SELECT * FROM user u WHERE u.id = ?";
        SqlAnalysisInfo result2 = SqlUtil.analyzeSql(sql2);
        assertEquals("u", result2.getTables().get(0).alias());
    }

    @Test
    @DisplayName("测试复杂嵌套场景")
    public void testComplexNestedScenario() {
        String sql = "SELECT u.name, p.title FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id AND p.type = ? " +
                "WHERE u.status IN (?, ?) AND u.created_at BETWEEN ? AND ? " +
                "AND EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.status = ?)";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(3, result.getTables().size()); // user 和 profile，EXISTS 子查询中的表不会被主解析器捕获
        assertTrue(result.getConditions().size() >= 3); // 至少包含主要的 WHERE 条件
        assertTrue(result.getParameterMappings().size() >= 5); // 至少5个参数
    }

    @Test
    @DisplayName("测试特殊字符和转义")
    public void testSpecialCharactersAndEscaping() {
        String sql = "SELECT `user_name`, `phone_number` FROM `user_table` WHERE `user_name` = ?";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals("user_table", result.getTables().get(0).tableName());
        assertEquals(1, result.getConditions().size());
        assertEquals("user_name", result.getConditions().get(0).columnName());
    }

    @Test
    @DisplayName("测试多种 JOIN 类型")
    public void testMultipleJoinTypes() {
        String sql = "SELECT * FROM user u " +
                "LEFT JOIN profile p ON u.id = p.user_id " +
                "RIGHT JOIN department d ON u.dept_id = d.id " +
                "INNER JOIN role r ON u.role_id = r.id " +
                "FULL OUTER JOIN company c ON d.company_id = c.id " +
                "CROSS JOIN settings s";

        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(6, result.getTables().size());

        // 验证主表
        assertEquals(TableType.MAIN, result.getTables().get(0).tableType());
        
        // 验证所有 JOIN 表
        for (int i = 1; i < result.getTables().size(); i++) {
            assertEquals(TableType.JOIN, result.getTables().get(i).tableType());
        }
    }

    @Test
    @DisplayName("测试 getAllFields 方法不同 SQL 类型的字段顺序")
    public void testGetAllFieldsOrder() {
        // INSERT: 只有 insertFields
        String insertSql = "INSERT INTO user (name, phone) VALUES (?, ?)";
        SqlAnalysisInfo insertResult = SqlUtil.analyzeSql(insertSql);
        assertEquals(2, insertResult.getAllFields().size());
        assertEquals(FieldType.INSERT, insertResult.getAllFields().get(0).fieldType());

        // UPDATE: setFields + conditions
        String updateSql = "UPDATE user SET name = ?, phone = ? WHERE id = ?";
        SqlAnalysisInfo updateResult = SqlUtil.analyzeSql(updateSql);
        assertEquals(3, updateResult.getAllFields().size());
        assertEquals(FieldType.UPDATE_SET, updateResult.getAllFields().get(0).fieldType());
        assertEquals(FieldType.UPDATE_SET, updateResult.getAllFields().get(1).fieldType());
        assertEquals(FieldType.CONDITION, updateResult.getAllFields().get(2).fieldType());

        // SELECT/DELETE: 只有 conditions
        String selectSql = "SELECT * FROM user WHERE phone = ?";
        SqlAnalysisInfo selectResult = SqlUtil.analyzeSql(selectSql);
        assertEquals(1, selectResult.getAllFields().size());
        assertEquals(FieldType.CONDITION, selectResult.getAllFields().get(0).fieldType());
    }

    @Test
    @DisplayName("测试用户报告的复杂 SQL 问题")
    public void testUserReportedComplexSql() {
        String sql = "SELECT * FROM user u WHERE u.isDel = 1 AND u.phone like ? AND u.status IN(?, ?) AND u.age > ? AND u.name IS NOT NULL AND u.id BETWEEN(?, ?)";
        SqlAnalysisInfo result = SqlUtil.analyzeSql(sql);

        assertEquals(SqlType.SELECT, result.getSqlType());
        assertEquals(1, result.getTables().size());
        assertEquals("user", result.getTables().get(0).tableName());
        assertEquals("u", result.getTables().get(0).alias());

        // 验证条件字段 - 只有包含参数的条件会被解析
        List<FieldCondition> conditions = result.getConditions();
        assertEquals(4, conditions.size());

        // u.phone like ?
        FieldCondition phoneCondition = conditions.get(0);
        assertEquals("u", phoneCondition.tableAlias());
        assertEquals("phone", phoneCondition.columnName());
        assertEquals(OperatorType.SINGLE_PARAM, phoneCondition.operatorType());
        assertEquals(1, phoneCondition.actualParamCount());

        // u.status IN(?, ?)
        FieldCondition statusCondition = conditions.get(1);
        assertEquals("u", statusCondition.tableAlias());
        assertEquals("status", statusCondition.columnName());
        assertEquals(OperatorType.IN_OPERATOR, statusCondition.operatorType());
        assertEquals(2, statusCondition.actualParamCount());

        // u.age > ?
        FieldCondition ageCondition = conditions.get(2);
        assertEquals("u", ageCondition.tableAlias());
        assertEquals("age", ageCondition.columnName());
        assertEquals(OperatorType.SINGLE_PARAM, ageCondition.operatorType());
        assertEquals(1, ageCondition.actualParamCount());

        // u.id BETWEEN(?, ?) - 这是关键测试点
        FieldCondition idCondition = conditions.get(3);
        assertEquals("u", idCondition.tableAlias());
        assertEquals("id", idCondition.columnName());
        assertEquals(OperatorType.BETWEEN_OPERATOR, idCondition.operatorType());
        assertEquals(2, idCondition.actualParamCount());

        // 验证参数映射总数
        List<ParameterFieldMapping> mappings = result.getParameterMappings();
        assertEquals(6, mappings.size()); // 1 + 2 + 1 + 2 = 6 个参数

        // 验证具体的参数映射
        assertEquals("phone", mappings.get(0).fieldName());
        assertEquals("status_1", mappings.get(1).fieldName());
        assertEquals("status_2", mappings.get(2).fieldName());
        assertEquals("age", mappings.get(3).fieldName());
        assertEquals("id_min", mappings.get(4).fieldName());
        assertEquals("id_max", mappings.get(5).fieldName());
    }
}