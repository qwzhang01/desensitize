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
}
