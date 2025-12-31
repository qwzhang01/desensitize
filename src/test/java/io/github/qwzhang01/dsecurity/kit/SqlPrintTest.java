package io.github.qwzhang01.dsecurity.kit;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlPrint 工具类的全面测试
 * 测试各种边界情况和潜在问题
 */
@DisplayName("SqlPrint 工具类测试")
class SqlPrintTest {

    private Configuration configuration;
    private SqlPrint sqlPrint;

    @BeforeEach
    void setUp() {
        configuration = new Configuration();
        sqlPrint = SqlPrint.getInstance();
    }

    @Test
    @DisplayName("测试单例模式")
    void testSingleton() {
        SqlPrint instance1 = SqlPrint.getInstance();
        SqlPrint instance2 = SqlPrint.getInstance();
        assertSame(instance1, instance2, "应该返回同一个实例");
    }

    /**
     * 【核心测试】测试包含美元符号和花括号的参数值
     * 这是导致原始错误的根本原因
     */
    @Test
    @DisplayName("测试包含 ${...} 的参数值（原始错误场景）")
    void testParameterWithDollarBraces() {
        String sql = "INSERT INTO log_notice (content, handleUrl) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "通知内容");
        params.put("arg1", "https://example.com/handle?id=${handleUrl}");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 100, 1);
        }, "包含 ${...} 的参数不应抛出异常");
    }

    @Test
    @DisplayName("测试包含美元符号和数字的参数值")
    void testParameterWithDollarNumbers() {
        String sql = "SELECT * FROM products WHERE price = ? AND code = ?";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "$100.50");
        params.put("arg1", "PROD$123");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 50, Collections.emptyList());
        }, "包含美元符号的参数不应抛出异常");
    }

    @Test
    @DisplayName("测试包含正则特殊字符的参数值")
    void testParameterWithRegexSpecialChars() {
        String sql = "INSERT INTO logs (pattern, replacement) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "$1 $2 $3");  // 正则反向引用
        params.put("arg1", "\\d+ \\s+ [a-z]+ (group) {1,3}");  // 正则表达式
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 30, 1);
        }, "包含正则特殊字符的参数不应抛出异常");
    }

    @Test
    @DisplayName("测试包含单引号的参数值")
    void testParameterWithSingleQuotes() {
        String sql = "INSERT INTO users (name, description) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "O'Neill");
        params.put("arg1", "It's a test");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 40, 1);
        }, "包含单引号的参数不应抛出异常");
    }

    @Test
    @DisplayName("测试包含反斜杠的参数值")
    void testParameterWithBackslashes() {
        String sql = "INSERT INTO paths (windows_path, regex) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "C:\\Users\\Admin\\Documents");
        params.put("arg1", "\\d+\\s+\\w+");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 20, 1);
        }, "包含反斜杠的参数不应抛出异常");
    }

    @Test
    @DisplayName("测试 NULL 参数")
    void testNullParameter() {
        String sql = "UPDATE users SET email = ? WHERE id = ?";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", null);
        params.put("arg1", 123);
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.update", 
                    System.currentTimeMillis() - 15, 1);
        }, "NULL 参数不应抛出异常");
    }

    @Test
    @DisplayName("测试数字类型参数")
    void testNumericParameters() {
        String sql = "SELECT * FROM orders WHERE amount = ? AND quantity = ? AND discount = ?";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", 99.99);
        params.put("arg1", 5);
        params.put("arg2", 10L);
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1", "arg2");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 25, Collections.emptyList());
        }, "数字参数不应抛出异常");
    }

    @Test
    @DisplayName("测试布尔类型参数")
    void testBooleanParameters() {
        String sql = "UPDATE users SET active = ?, verified = ? WHERE id = ?";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", true);
        params.put("arg1", false);
        params.put("arg2", 1);
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1", "arg2");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.update", 
                    System.currentTimeMillis() - 10, 1);
        }, "布尔参数不应抛出异常");
    }

    @Test
    @DisplayName("测试日期时间类型参数")
    void testDateTimeParameters() {
        String sql = "INSERT INTO events (create_date, update_time, event_date, event_time) VALUES (?, ?, ?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", new Date());
        params.put("arg1", LocalDateTime.now());
        params.put("arg2", LocalDate.now());
        params.put("arg3", LocalTime.now());
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1", "arg2", "arg3");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 35, 1);
        }, "日期时间参数不应抛出异常");
    }

    @Test
    @DisplayName("测试枚举类型参数")
    void testEnumParameters() {
        String sql = "INSERT INTO settings (level, status) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", TestEnum.LEVEL_ONE);
        params.put("arg1", TestEnum.STATUS_ACTIVE);
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 18, 1);
        }, "枚举参数不应抛出异常");
    }

    @Test
    @DisplayName("测试空 SQL")
    void testEmptySql() {
        BoundSql boundSql = createBoundSql("", new HashMap<>());
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.empty", 
                    System.currentTimeMillis(), null);
        }, "空 SQL 不应抛出异常");
    }

    @Test
    @DisplayName("测试 null SQL")
    void testNullSql() {
        BoundSql boundSql = createBoundSql(null, new HashMap<>());
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.null", 
                    System.currentTimeMillis(), null);
        }, "null SQL 不应抛出异常");
    }

    @Test
    @DisplayName("测试无参数 SQL")
    void testSqlWithoutParameters() {
        String sql = "SELECT * FROM users WHERE status = 'active'";
        BoundSql boundSql = createBoundSql(sql, new HashMap<>());
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 12, Collections.emptyList());
        }, "无参数 SQL 不应抛出异常");
    }

    @Test
    @DisplayName("测试占位符数量不匹配")
    void testMismatchedPlaceholders() {
        String sql = "SELECT * FROM users WHERE id = ? AND name = ? AND age = ?";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", 1);
        params.put("arg1", "张三");
        // 缺少第三个参数
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 22, Collections.emptyList());
        }, "占位符不匹配应该记录警告但不抛出异常");
    }

    @Test
    @DisplayName("测试多行 SQL 语句")
    void testMultilineSql() {
        String sql = """
                SELECT u.id, u.name, u.email
                FROM users u
                WHERE u.status = ?
                  AND u.created_at > ?
                ORDER BY u.id DESC
                """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "active");
        params.put("arg1", LocalDateTime.now().minusDays(30));
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 45, Collections.emptyList());
        }, "多行 SQL 不应抛出异常");
    }

    @Test
    @DisplayName("测试包含 SQL 注释的语句")
    void testSqlWithComments() {
        String sql = """
                -- 查询活跃用户
                SELECT * FROM users
                WHERE status = ? /* 状态参数 */
                """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "active");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 8, Collections.emptyList());
        }, "包含注释的 SQL 不应抛出异常");
    }

    @Test
    @DisplayName("测试不同返回结果类型")
    void testDifferentResultTypes() {
        String sql = "SELECT * FROM users WHERE id = ?";
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", 1);
        BoundSql boundSql = createBoundSql(sql, params, "arg0");
        
        // 测试 Collection 结果
        assertDoesNotThrow(() -> {
            List<Object> list = Arrays.asList(new Object(), new Object());
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 5, list);
        }, "Collection 结果不应抛出异常");
        
        // 测试 Number 结果
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.update", 
                    System.currentTimeMillis() - 3, 5);
        }, "Number 结果不应抛出异常");
        
        // 测试 null 结果
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 2, null);
        }, "null 结果不应抛出异常");
        
        // 测试其他对象结果
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.select", 
                    System.currentTimeMillis() - 1, new Object());
        }, "Object 结果不应抛出异常");
    }

    @Test
    @DisplayName("测试超长参数值")
    void testVeryLongParameterValue() {
        String sql = "INSERT INTO logs (message) VALUES (?)";
        
        String longMessage = "A".repeat(10000);
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", longMessage);
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 100, 1);
        }, "超长参数值不应抛出异常");
    }

    @Test
    @DisplayName("测试特殊 Unicode 字符")
    void testUnicodeCharacters() {
        String sql = "INSERT INTO messages (content, emoji) VALUES (?, ?)";
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "你好世界 🌍");
        params.put("arg1", "🎉🎊🎈😀");
        
        BoundSql boundSql = createBoundSql(sql, params, "arg0", "arg1");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 16, 1);
        }, "Unicode 字符不应抛出异常");
    }

    @Test
    @DisplayName("测试混合所有特殊情况")
    void testMixedSpecialCases() {
        String sql = """
                INSERT INTO complex_table 
                (str_dollar, str_quote, str_backslash, num_val, bool_val, 
                 date_val, null_val, enum_val)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("arg0", "${variable} $1 $2");
        params.put("arg1", "O'Brien's \"test\"");
        params.put("arg2", "C:\\path\\to\\file");
        params.put("arg3", 99.99);
        params.put("arg4", true);
        params.put("arg5", LocalDateTime.now());
        params.put("arg6", null);
        params.put("arg7", TestEnum.LEVEL_ONE);
        
        BoundSql boundSql = createBoundSql(sql, params, 
                "arg0", "arg1", "arg2", "arg3", "arg4", "arg5", "arg6", "arg7");
        
        assertDoesNotThrow(() -> {
            sqlPrint.print(configuration, boundSql, "test.insert", 
                    System.currentTimeMillis() - 60, 1);
        }, "混合特殊情况不应抛出异常");
    }

    // ========== 辅助方法 ==========

    /**
     * 创建 BoundSql 对象用于测试
     */
    private BoundSql createBoundSql(String sql, Map<String, Object> params, String... paramNames) {
        SqlSource sqlSource = new SqlSource() {
            @Override
            public BoundSql getBoundSql(Object parameterObject) {
                List<ParameterMapping> mappings = new ArrayList<>();
                for (String name : paramNames) {
                    ParameterMapping.Builder builder = new ParameterMapping.Builder(
                            configuration, name, Object.class);
                    mappings.add(builder.build());
                }
                return new BoundSql(configuration, sql == null ? "" : sql, 
                        mappings, parameterObject);
            }
        };
        
        return sqlSource.getBoundSql(params.isEmpty() ? null : params);
    }

    /**
     * 测试用枚举
     */
    enum TestEnum {
        LEVEL_ONE,
        LEVEL_TWO,
        STATUS_ACTIVE,
        STATUS_INACTIVE
    }
}
