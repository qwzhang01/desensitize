package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 解析工具
 *
 * @author avinzhang
 */
public final class SqlUtil {
    private static final Logger log = LoggerFactory.getLogger(SqlUtil.class);

    // SQL 解析相关的正则表达式
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(?:FROM|JOIN|UPDATE|INTO)\\s+([\\w_]+)(?:\\s+(?:AS\\s+)?([\\w_]+))?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WHERE_CONDITION_PATTERN = Pattern.compile(
            "(?i)\\b([\\w_]+\\.)?([\\w_]+)\\s*(?:=|!=|<>|LIKE|IN|NOT\\s+IN)\\s*\\?",
            Pattern.CASE_INSENSITIVE
    );

    // UPDATE 语句 SET 子句的正则表达式
    private static final Pattern UPDATE_SET_PATTERN = Pattern.compile(
            "(?i)\\bSET\\s+(.*?)\\s+WHERE",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern SET_FIELD_PATTERN = Pattern.compile(
            "(?i)(?:`([\\w_]+)`|([\\w_]+))\\s*=\\s*\\?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 分析 SQL 语句，提取表信息和字段信息
     */
    public static SqlAnalysisInfo analyzeSql(String sql) {
        SqlAnalysisInfo result = new SqlAnalysisInfo();

        // 解析表名和别名
        Matcher tableMatcher = TABLE_PATTERN.matcher(sql);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1);
            String alias = tableMatcher.group(2);
            if (alias != null) {
                if ("WHERE".equalsIgnoreCase(alias.trim()) || "ON".equalsIgnoreCase(alias.trim())) {
                    alias = "";
                }
            }


            SqlAnalysisInfo.TableInfo tableInfo = new SqlAnalysisInfo.TableInfo(tableName, alias);
            result.addTable(tableInfo);

            log.debug("发现表: {} (别名: {})", tableName, alias);
        }

        // 检查是否为 UPDATE 语句，如果是则解析 SET 子句
        if (sql.trim().toUpperCase().startsWith("UPDATE")) {
            parseUpdateSetClause(sql, result);
        }

        // 解析 WHERE 条件中的字段
        Matcher conditionMatcher = WHERE_CONDITION_PATTERN.matcher(sql);
        while (conditionMatcher.find()) {
            String tableAlias = conditionMatcher.group(1);
            String columnName = conditionMatcher.group(2);

            if (tableAlias != null) {
                tableAlias = tableAlias.replace(".", "");
            }

            SqlAnalysisInfo.FieldCondition condition = new SqlAnalysisInfo.FieldCondition(tableAlias, columnName);
            result.addCondition(condition);

            log.debug("发现查询条件字段: {}.{}", tableAlias, columnName);
        }

        return result;
    }

    /**
     * 解析 UPDATE 语句的 SET 子句
     */
    private static void parseUpdateSetClause(String sql, SqlAnalysisInfo result) {
        // 提取 SET 子句内容
        Matcher setMatcher = UPDATE_SET_PATTERN.matcher(sql);
        if (setMatcher.find()) {
            String setClause = setMatcher.group(1);
            log.debug("解析 SET 子句: {}", setClause);

            // 解析 SET 子句中的字段
            Matcher fieldMatcher = SET_FIELD_PATTERN.matcher(setClause);
            while (fieldMatcher.find()) {
                // group(1) 是反引号包围的字段名，group(2) 是普通字段名
                String columnName = fieldMatcher.group(1) != null ? fieldMatcher.group(1) : fieldMatcher.group(2);
                
                SqlAnalysisInfo.FieldCondition condition = new SqlAnalysisInfo.FieldCondition(null, columnName);
                result.addSetField(condition);

                log.debug("发现 SET 字段: {}", columnName);
            }
        } else {
            // 如果没有 WHERE 子句，尝试匹配整个 SET 部分
            Pattern setOnlyPattern = Pattern.compile("(?i)\\bSET\\s+(.*?)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher setOnlyMatcher = setOnlyPattern.matcher(sql);
            if (setOnlyMatcher.find()) {
                String setClause = setOnlyMatcher.group(1);
                log.debug("解析 SET 子句（无WHERE）: {}", setClause);

                Matcher fieldMatcher = SET_FIELD_PATTERN.matcher(setClause);
                while (fieldMatcher.find()) {
                    String columnName = fieldMatcher.group(1) != null ? fieldMatcher.group(1) : fieldMatcher.group(2);
                    
                    SqlAnalysisInfo.FieldCondition condition = new SqlAnalysisInfo.FieldCondition(null, columnName);
                    result.addSetField(condition);

                    log.debug("发现 SET 字段（无WHERE）: {}", columnName);
                }
            }
        }
    }
}
