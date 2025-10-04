package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo;
import io.github.qwzhang01.desensitize.domain.SqlAnalysisInfo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;

import static io.github.qwzhang01.desensitize.kit.RegexPatterns.*;

/**
 * 增强的 SQL 解析工具
 * 支持 SELECT、INSERT、UPDATE、DELETE 四种类型的复杂 SQL 语句
 * 包括嵌套查询、关联查询、子查询等复杂场景
 *
 * @author avinzhang
 */
public final class SqlUtil {
    private static final Logger log = LoggerFactory.getLogger(SqlUtil.class);

    private SqlUtil() {
        // 工具类不允许实例化
    }

    /**
     * 分析 SQL 语句，提取表信息和字段信息
     *
     * @param sql SQL语句
     * @return SQL分析结果
     */
    public static SqlAnalysisInfo analyzeSql(String sql) {
        if (StringUtil.isEmpty(sql)) {
            return new SqlAnalysisInfo();
        }

        log.debug("开始解析 SQL: {}", sql);

        SqlAnalysisInfo result = new SqlAnalysisInfo();

        // 预处理 SQL：移除注释和多余空格
        String cleanSql = preprocessSql(sql);

        // 确定 SQL 类型
        SqlType sqlType = determineSqlType(cleanSql);
        result.setSqlType(sqlType);

        log.debug("SQL 类型: {}", sqlType);

        // 根据 SQL 类型进行不同的解析
        switch (sqlType) {
            case SELECT:
                parseSelectStatement(cleanSql, result);
                break;
            case INSERT:
                parseInsertStatement(cleanSql, result);
                break;
            case UPDATE:
                parseUpdateStatement(cleanSql, result);
                break;
            case DELETE:
                parseDeleteStatement(cleanSql, result);
                break;
        }

        // 解析参数占位符与字段的映射关系
        buildParameterMappings(result);

        log.debug("SQL 解析完成，发现 {} 个表，{} 个参数映射",
                result.getTables().size(), result.getParameterMappings().size());

        return result;
    }

    /**
     * 预处理 SQL：移除注释和规范化空格
     */
    private static String preprocessSql(String sql) {
        // 移除单行注释
        sql = sql.replaceAll("--.*", "");
        // 移除多行注释
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        // 规范化空格
        sql = sql.replaceAll("\\s+", " ").trim();
        return sql;
    }

    /**
     * 确定 SQL 语句类型
     */
    private static SqlType determineSqlType(String sql) {
        if (SELECT_PATTERN.matcher(sql).find()) {
            return SqlType.SELECT;
        } else if (INSERT_PATTERN.matcher(sql).find()) {
            return SqlType.INSERT;
        } else if (UPDATE_PATTERN.matcher(sql).find()) {
            return SqlType.UPDATE;
        } else if (DELETE_PATTERN.matcher(sql).find()) {
            return SqlType.DELETE;
        }

        // 默认返回 SELECT
        return SqlType.SELECT;
    }

    /**
     * 解析 SELECT 语句
     */
    private static void parseSelectStatement(String sql, SqlAnalysisInfo result) {
        log.debug("解析 SELECT 语句");

        // 解析 FROM 子句中的表
        parseFromClause(sql, result);

        // 解析 JOIN 子句中的表
        parseJoinClause(sql, result);

        // 解析 SELECT 字段
        parseSelectFields(sql, result);

        // 解析 WHERE 条件
        parseWhereConditions(sql, result);

        // 解析 JOIN ON 条件
        parseJoinOnConditions(sql, result);
    }

    /**
     * 解析 INSERT 语句
     */
    private static void parseInsertStatement(String sql, SqlAnalysisInfo result) {
        log.debug("解析 INSERT 语句");

        // 解析目标表
        parseInsertTable(sql, result);

        // 解析插入字段
        parseInsertFields(sql, result);
    }

    /**
     * 解析 UPDATE 语句
     */
    private static void parseUpdateStatement(String sql, SqlAnalysisInfo result) {
        log.debug("解析 UPDATE 语句");

        // 解析目标表
        parseUpdateTable(sql, result);

        // 解析 SET 字段
        parseUpdateSetClause(sql, result);

        // 解析 WHERE 条件
        parseWhereConditions(sql, result);
    }

    /**
     * 解析 DELETE 语句
     */
    private static void parseDeleteStatement(String sql, SqlAnalysisInfo result) {
        log.debug("解析 DELETE 语句");

        // 解析目标表
        parseDeleteTable(sql, result);

        // 解析 WHERE 条件
        parseWhereConditions(sql, result);
    }

    /**
     * 解析 FROM 子句
     */
    private static void parseFromClause(String sql, SqlAnalysisInfo result) {
        Matcher matcher = SELECT_FROM_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableName = getFirstNonNull(matcher.group(1), matcher.group(2));
            String alias = getFirstNonNull(matcher.group(3), matcher.group(4));

            if (tableName != null) {
                TableInfo tableInfo = new TableInfo(tableName, cleanAlias(alias), TableType.MAIN);
                result.addTable(tableInfo);
                log.debug("发现主表: {} (别名: {})", tableName, alias);
            }
        }
    }

    /**
     * 解析 JOIN 子句
     */
    private static void parseJoinClause(String sql, SqlAnalysisInfo result) {
        Matcher matcher = JOIN_PATTERN.matcher(sql);
        while (matcher.find()) {
            String tableName = getFirstNonNull(matcher.group(1), matcher.group(2));
            String alias = getFirstNonNull(matcher.group(3), matcher.group(4));

            if (tableName != null) {
                TableInfo tableInfo = new TableInfo(tableName, cleanAlias(alias), TableType.JOIN);
                result.addTable(tableInfo);
                log.debug("发现 JOIN 表: {} (别名: {})", tableName, alias);
            }
        }
    }

    /**
     * 解析 SELECT 字段
     */
    private static void parseSelectFields(String sql, SqlAnalysisInfo result) {
        Matcher matcher = SELECT_FIELDS_PATTERN.matcher(sql);
        if (matcher.find()) {
            String fieldsClause = matcher.group(1);
            log.debug("解析 SELECT 字段: {}", fieldsClause);

            // 这里可以进一步解析具体的字段，但对于参数加密来说不是必需的
            // 因为 SELECT 语句的参数主要在 WHERE 条件中
        }
    }

    /**
     * 解析 INSERT 目标表
     */
    private static void parseInsertTable(String sql, SqlAnalysisInfo result) {
        Matcher matcher = INSERT_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = getFirstNonNull(matcher.group(1), matcher.group(2));
            if (tableName != null) {
                TableInfo tableInfo = new TableInfo(tableName, null, TableType.MAIN);
                result.addTable(tableInfo);
                log.debug("发现 INSERT 目标表: {}", tableName);
            }
        }
    }

    /**
     * 解析 INSERT 字段
     */
    private static void parseInsertFields(String sql, SqlAnalysisInfo result) {
        Matcher matcher = INSERT_FIELDS_PATTERN.matcher(sql);
        if (matcher.find()) {
            String fieldsClause = matcher.group(1);
            log.debug("解析 INSERT 字段: {}", fieldsClause);

            // 解析字段列表
            String[] fields = fieldsClause.split(",");
            for (String field : fields) {
                String cleanField = field.trim().replaceAll("`", "");
                if (!cleanField.isEmpty()) {
                    FieldCondition fieldCondition = new FieldCondition(null, cleanField, FieldType.INSERT, OperatorType.SINGLE_PARAM, 1);
                    result.addInsertField(fieldCondition);
                    log.debug("发现 INSERT 字段: {}", cleanField);
                }
            }
        }
    }

    /**
     * 解析 UPDATE 目标表
     */
    private static void parseUpdateTable(String sql, SqlAnalysisInfo result) {
        Matcher matcher = UPDATE_PATTERN.matcher(sql);
        if (matcher.find()) {
            // 提取 UPDATE 后面的表名
            String updateClause = sql.substring(matcher.end()).trim();
            String[] parts = updateClause.split("\\s+");
            if (parts.length > 0) {
                String tableName = parts[0].replaceAll("`", "");
                String alias = parts.length > 1 && !parts[1].toUpperCase().equals("SET") ? parts[1] : null;

                TableInfo tableInfo = new TableInfo(tableName, cleanAlias(alias), TableType.MAIN);
                result.addTable(tableInfo);
                log.debug("发现 UPDATE 目标表: {} (别名: {})", tableName, alias);
            }
        }
    }

    /**
     * 解析 DELETE 目标表
     */
    private static void parseDeleteTable(String sql, SqlAnalysisInfo result) {
        Matcher matcher = DELETE_TABLE_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = getFirstNonNull(matcher.group(1), matcher.group(2));
            String alias = getFirstNonNull(matcher.group(3), matcher.group(4));

            if (tableName != null) {
                TableInfo tableInfo = new TableInfo(tableName, cleanAlias(alias), TableType.MAIN);
                result.addTable(tableInfo);
                log.debug("发现 DELETE 目标表: {} (别名: {})", tableName, alias);
            }
        }
    }

    /**
     * 解析 UPDATE SET 子句
     */
    private static void parseUpdateSetClause(String sql, SqlAnalysisInfo result) {
        Matcher matcher = UPDATE_SET_PATTERN.matcher(sql);
        if (matcher.find()) {
            String setClause = matcher.group(1);
            log.debug("解析 SET 子句: {}", setClause);

            Matcher fieldMatcher = SET_FIELD_PATTERN.matcher(setClause);
            while (fieldMatcher.find()) {
                String fieldName = getFirstNonNull(fieldMatcher.group(1), fieldMatcher.group(2));
                if (fieldName != null) {
                    FieldCondition fieldCondition = new FieldCondition(null, fieldName, FieldType.UPDATE_SET, OperatorType.SINGLE_PARAM, 1);
                    result.addSetField(fieldCondition);
                    log.debug("发现 SET 字段: {}", fieldName);
                }
            }
        }
    }

    /**
     * 解析 WHERE 条件
     */
    private static void parseWhereConditions(String sql, SqlAnalysisInfo result) {
        // 提取 WHERE 子句部分
        String whereClause = extractWhereClause(sql);
        if (whereClause == null || whereClause.trim().isEmpty()) {
            log.debug("未发现 WHERE 子句");
            return;
        }

        log.debug("解析 WHERE 子句: {}", whereClause);

        // 使用统一的正则表达式找到所有条件
        Matcher matcher = WHERE_CONDITION_PATTERN.matcher(whereClause);
        while (matcher.find()) {
            String matchedText = matcher.group(0);
            
            // 使用改进的方法解析每个条件
            ConditionInfo conditionInfo = parseCondition(matchedText);
            
            if (conditionInfo.fieldName != null) {
                FieldCondition condition = new FieldCondition(
                    conditionInfo.tableAlias, 
                    conditionInfo.fieldName, 
                    FieldType.CONDITION, 
                    conditionInfo.operatorType, 
                    conditionInfo.actualParamCount
                );
                result.addCondition(condition);
                log.debug("发现 WHERE 条件字段: {}.{} 操作符:{} 参数数量:{}", 
                    conditionInfo.tableAlias, conditionInfo.fieldName, conditionInfo.operatorType, conditionInfo.actualParamCount);
            }
        }
    }

    /**
     * 解析单个条件的信息
     * 使用更清晰的逻辑来确定操作符类型和提取字段信息
     */
    private static ConditionInfo parseCondition(String condition) {
        ConditionInfo info = new ConditionInfo();
        
        // 按优先级检查不同的操作符类型
        Matcher matcher;
        
        // 1. 检查 IN 操作符
        matcher = RegexPatterns.IN_OPERATOR_PATTERN.matcher(condition);
        if (matcher.matches()) {
            info.operatorType = OperatorType.IN_OPERATOR;
            info.tableAlias = getFirstNonNull(matcher.group(1), matcher.group(2));
            info.fieldName = getFirstNonNull(matcher.group(3), matcher.group(4));
            info.actualParamCount = countParametersInMatch(condition);
            return info;
        }
        
        // 2. 检查 BETWEEN 操作符
        matcher = RegexPatterns.BETWEEN_OPERATOR_PATTERN.matcher(condition);
        if (matcher.matches()) {
            info.operatorType = OperatorType.BETWEEN_OPERATOR;
            info.tableAlias = getFirstNonNull(matcher.group(1), matcher.group(2));
            info.fieldName = getFirstNonNull(matcher.group(3), matcher.group(4));
            info.actualParamCount = 2;
            return info;
        }
        
        // 3. 检查普通操作符
        matcher = RegexPatterns.SINGLE_PARAM_PATTERN.matcher(condition);
        if (matcher.matches()) {
            info.operatorType = OperatorType.SINGLE_PARAM;
            info.tableAlias = getFirstNonNull(matcher.group(1), matcher.group(2));
            info.fieldName = getFirstNonNull(matcher.group(3), matcher.group(4));
            info.actualParamCount = 1;
            return info;
        }
        
        // 如果都不匹配，返回未知类型
        info.operatorType = OperatorType.SINGLE_PARAM; // 默认为单参数
        return info;
    }

    /**
     * 条件信息内部类
     */
    private static class ConditionInfo {
        String tableAlias;
        String fieldName;
        OperatorType operatorType;
        int actualParamCount;
    }


    /**
     * 提取 WHERE 子句内容
     * 从 SQL 语句中提取 WHERE 关键字之后到语句结束或其他子句开始的部分
     */
    private static String extractWhereClause(String sql) {
        Matcher matcher = WHERE_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 构建参数占位符与字段的映射关系
     */
    private static void buildParameterMappings(SqlAnalysisInfo result) {
        List<FieldCondition> allFields = result.getAllFields();
        int parameterIndex = 0;

        for (FieldCondition field : allFields) {
            String tableName = determineTableName(field, result);

            // 根据操作符类型创建对应数量的参数映射
            int paramCount = field.getEffectiveParamCount();

            for (int i = 0; i < paramCount; i++) {
                String fieldSuffix = "";
                if (paramCount > 1) {
                    // 对于多参数操作符，添加后缀区分
                    switch (field.operatorType()) {
                        case IN_OPERATOR:
                            fieldSuffix = "_" + (i + 1);
                            break;
                        case BETWEEN_OPERATOR:
                            fieldSuffix = i == 0 ? "_min" : "_max";
                            break;
                    }
                }

                ParameterFieldMapping mapping = new ParameterFieldMapping(
                        parameterIndex++,
                        tableName,
                        field.columnName() + fieldSuffix,
                        field.tableAlias(),
                        field.fieldType()
                );

                result.addParameterMapping(mapping);
                log.debug("参数映射: 索引[{}] -> 表[{}] 字段[{}] 类型[{}] 操作符[{}]",
                        mapping.parameterIndex(), mapping.tableName(),
                        mapping.fieldName(), mapping.fieldType(), field.operatorType());
            }
        }
    }

    /**
     * 确定字段所属的表名
     */
    private static String determineTableName(FieldCondition field, SqlAnalysisInfo result) {
        // 如果有表别名，先通过别名查找
        if (field.tableAlias() != null) {
            String realTableName = result.getRealTableName(field.tableAlias());
            if (realTableName != null) {
                return realTableName;
            }
        }

        // 如果没有别名或别名查找失败，使用第一个表（通常是主表）
        List<TableInfo> tables = result.getTables();
        if (!tables.isEmpty()) {
            return tables.get(0).tableName();
        }

        return "unknown_table";
    }

    /**
     * 获取第一个非空值
     */
    private static String getFirstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 清理别名，排除SQL关键字
     */
    private static String cleanAlias(String alias) {
        if (alias == null) {
            return null;
        }

        String trimmed = alias.trim().toUpperCase();
        // 排除常见的SQL关键字
        if ("WHERE".equals(trimmed) || "ON".equals(trimmed) || "SET".equals(trimmed) ||
                "VALUES".equals(trimmed) || "ORDER".equals(trimmed) || "GROUP".equals(trimmed) ||
                "HAVING".equals(trimmed) || "LIMIT".equals(trimmed)) {
            return null;
        }

        return alias.trim();
    }

    /**
     * 解析 JOIN ON 条件
     */
    private static void parseJoinOnConditions(String sql, SqlAnalysisInfo result) {
        Matcher joinMatcher = JOIN_ON_PATTERN.matcher(sql);
        while (joinMatcher.find()) {
            String onClause = joinMatcher.group(1);
            if (onClause == null || onClause.trim().isEmpty()) {
                continue;
            }

            log.debug("解析 JOIN ON 条件: {}", onClause);

            // 使用专门的 ON 条件模式解析字段
            Matcher conditionMatcher = ON_CONDITION_PATTERN.matcher(onClause);
            while (conditionMatcher.find()) {
                String matchedText = conditionMatcher.group(0);
                
                // 使用改进的方法解析每个 ON 条件
                ConditionInfo conditionInfo = parseCondition(matchedText);
                
                if (conditionInfo.fieldName != null) {
                    FieldCondition condition = new FieldCondition(
                        conditionInfo.tableAlias, 
                        conditionInfo.fieldName, 
                        FieldType.CONDITION, 
                        conditionInfo.operatorType, 
                        conditionInfo.actualParamCount
                    );
                    result.addCondition(condition);
                    log.debug("发现 JOIN ON 条件字段: {}.{} 操作符:{} 参数数量:{}", 
                        conditionInfo.tableAlias, conditionInfo.fieldName, conditionInfo.operatorType, conditionInfo.actualParamCount);
                }
            }
        }
    }

    /**
     * 计算匹配文本中的参数个数
     */
    private static int countParametersInMatch(String matchedText) {
        int count = 0;
        for (int i = 0; i < matchedText.length(); i++) {
            if (matchedText.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }
}