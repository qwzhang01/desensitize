package io.github.qwzhang01.desensitize.kit;

import java.util.regex.Pattern;

/**
 * 正则表达式模式常量类
 *
 * @author avinzhang
 */
public final class RegexPatterns {
    /**
     * 统一的 WHERE 条件字段模式 - 同时处理带表前缀和不带表前缀的情况
     * 支持反引号和普通标识符
     */
    public static final Pattern WHERE_CONDITION_PATTERN = Pattern.compile(
            "(?i)(?:(?:`([\\w_]+)`|([\\w_]+))\\.)?(?:`([\\w_]+)`|([\\w_]+))\\s*(?:=|!=|<>|<|>|<=|>=|LIKE|NOT\\s+LIKE|IN|NOT\\s+IN|IS|IS\\s+NOT)\\s*\\?",
            Pattern.CASE_INSENSITIVE
    );

    // ========== SQL 解析相关正则表达式 ==========
    /**
     * SELECT 语句中的表名模式 - 专门处理 SELECT 语句的 FROM 子句
     */
    public static final Pattern SELECT_FROM_PATTERN = Pattern.compile(
            "(?i)\\bFROM\\s+(?:`([\\w_]+)`|([\\w_]+))(?:\\s+(?:AS\\s+)?(?:`([\\w_]+)`|([\\w_]+)))?",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * INSERT 语句表名模式
     */
    public static final Pattern INSERT_TABLE_PATTERN = Pattern.compile(
            "(?i)\\bINTO\\s+(?:`([\\w_]+)`|([\\w_]+))",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * DELETE 语句表名模式
     */
    public static final Pattern DELETE_TABLE_PATTERN = Pattern.compile(
            "(?i)\\bDELETE\\s+FROM\\s+(?:`([\\w_]+)`|([\\w_]+))(?:\\s+(?:AS\\s+)?(?:`([\\w_]+)`|([\\w_]+)))?",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * JOIN 表名模式 - 匹配各种类型的 JOIN
     */
    public static final Pattern JOIN_PATTERN = Pattern.compile(
            "(?i)\\b(?:LEFT|RIGHT|INNER|FULL|CROSS)?\\s*(?:OUTER\\s+)?JOIN\\s+" +
                    "(?:`([\\w_]+)`|([\\w_]+))(?:\\s+(?:AS\\s+)?(?:`([\\w_]+)`|([\\w_]+)))?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * UPDATE SET 子句模式 - 匹配 UPDATE 语句的 SET 子句
     */
    public static final Pattern UPDATE_SET_PATTERN = Pattern.compile(
            "(?i)\\bSET\\s+(.*?)\\s+(?:WHERE|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    /**
     * SET 字段模式 - 匹配 SET 子句中的字段赋值
     */
    public static final Pattern SET_FIELD_PATTERN = Pattern.compile(
            "(?i)(?:`([\\w_]+)`|([\\w_]+))\\s*=\\s*\\?",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * INSERT 字段列表模式 - 匹配 INSERT 语句的字段列表
     */
    public static final Pattern INSERT_FIELDS_PATTERN = Pattern.compile(
            "(?i)\\bINTO\\s+(?:`[\\w_]+`|[\\w_]+)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * INSERT VALUES 模式 - 匹配 INSERT 语句的 VALUES 部分
     */
    public static final Pattern INSERT_VALUES_PATTERN = Pattern.compile(
            "(?i)\\bVALUES\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * SELECT 字段模式 - 匹配 SELECT 语句的字段列表
     */
    public static final Pattern SELECT_FIELDS_PATTERN = Pattern.compile(
            "(?i)\\bSELECT\\s+(.*?)\\s+FROM",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    /**
     * 子查询模式 - 匹配括号中的子查询
     */
    public static final Pattern SUBQUERY_PATTERN = Pattern.compile(
            "\\(\\s*(?i:SELECT)\\s+.*?\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    /**
     * SQL 语句类型检测模式
     */
    public static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE\\b", Pattern.CASE_INSENSITIVE);
    public static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE\\b", Pattern.CASE_INSENSITIVE);
    /**
     * 字段名提取模式 - 从复杂的字段表达式中提取字段名
     */
    public static final Pattern FIELD_NAME_PATTERN = Pattern.compile(
            "(?i)(?:`([\\w_]+)`|([\\w_]+))(?:\\s+(?:AS\\s+)?(?:`([\\w_]+)`|([\\w_]+)))?",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * 表别名引用模式 - 匹配 table.field 格式
     */
    public static final Pattern TABLE_FIELD_PATTERN = Pattern.compile(
            "(?i)(?:`([\\w_]+)`|([\\w_]+))\\.(?:`([\\w_]+)`|([\\w_]+))",
            Pattern.CASE_INSENSITIVE
    );
    /**
     * QueryWrapper 等值条件模式 - field_name = #{ew.paramNameValuePairs.MPGENVAL1}
     */
    public static final Pattern QW_EQUAL_PATTERN = Pattern.compile(
            "(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );

    // ========== QueryWrapper 参数解析相关正则表达式 ==========
    /**
     * QueryWrapper LIKE 条件模式 - field_name LIKE #{ew.paramNameValuePairs.MPGENVAL1}
     */
    public static final Pattern QW_LIKE_PATTERN = Pattern.compile(
            "(\\w+)\\s+LIKE\\s+#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );
    /**
     * QueryWrapper IN 条件模式 - field_name IN (#{ew.paramNameValuePairs.MPGENVAL1}, ...)
     */
    public static final Pattern QW_IN_PATTERN = Pattern.compile(
            "(\\w+)\\s+IN\\s*\\([^)]*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );
    // 使用正则表达式匹配 WHERE 子句
    public static final Pattern WHERE_PATTERN = Pattern.compile(
            "(?i)\\bWHERE\\s+(.*?)(?:\\s+(?:ORDER\\s+BY|GROUP\\s+BY|HAVING|LIMIT|UNION|;)\\s+|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private RegexPatterns() {
        // 常量类不允许实例化
    }
}