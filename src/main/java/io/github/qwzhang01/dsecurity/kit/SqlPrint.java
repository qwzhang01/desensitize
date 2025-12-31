package io.github.qwzhang01.dsecurity.kit;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 改进版 SQL 打印工具类
 * 修复了原版的多项问题，提升了健壮性、准确性和可维护性
 * 
 * <p>主要修复：</p>
 * <ul>
 *   <li>修复参数值包含 $、${...} 等特殊字符导致的正则引用错误</li>
 *   <li>增强了对各种数据类型的处理（LocalDate、LocalTime、枚举等）</li>
 *   <li>优化了字符串转义逻辑，支持反斜杠转义</li>
 *   <li>改进了空白字符处理，避免破坏字符串字面量</li>
 * </ul>
 * 
 * @author avinzhang
 */
public class SqlPrint {
    private static final Logger log = LoggerFactory.getLogger(SqlPrint.class);

    // 线程安全的日期格式化器
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(
                    () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            );
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // 用于压缩多余空白，但保留字符串内容
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private SqlPrint() {
    }

    public static SqlPrint getInstance() {
        return Holder.INSTANCE;
    }

    public void print(Configuration configuration, BoundSql boundSql,
                      String sqlId, long startTime, Object result) {
        try {
            String sql = getSql(configuration, boundSql);
            if (sql.isEmpty()) {
                return;
            }
            printSql(sqlId, sql, System.currentTimeMillis() - startTime,
                    result);
        } catch (Exception e) {
            log.error("print sql error", e);
        }
    }

    /**
     * 构建带参数值的完整 SQL
     */
    private String getSql(Configuration configuration, BoundSql boundSql) {
        String sql = boundSql.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        // 统一空白字符为单个空格，便于阅读（使用非正则方法避免特殊字符问题）
        sql = compressWhitespace(sql);

        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings =
                boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry =
                configuration.getTypeHandlerRegistry();

        List<String> parameters = new java.util.ArrayList<>();

        if (parameterMappings != null && !parameterMappings.isEmpty()) {
            MetaObject metaObject = parameterObject == null ? null :
                    configuration.newMetaObject(parameterObject);

            for (ParameterMapping mapping : parameterMappings) {
                if (mapping.getMode() == ParameterMode.OUT) {
                    continue;
                }

                String propertyName = mapping.getProperty();
                Object value;

                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    value = metaObject == null ? null :
                            metaObject.getValue(propertyName);
                }

                parameters.add(formatParameterValue(value));
            }
        }

        // 校验占位符数量（可选，预防极端情况）
        int placeholderCount = countChar(sql, '?');
        if (placeholderCount != parameters.size()) {
            log.warn("SQL 占位符数量({}) 与参数数量({}) 不匹配: 占位符={}, 参数={}",
                    placeholderCount, parameters.size(), 
                    placeholderCount, parameters.size());
        }

        // 逐个替换 ? (使用 StringBuilder 非正则替换，避免 $ 等特殊字符问题)
        return replacePlaceholders(sql, parameters);
    }

    /**
     * 格式化参数值为 SQL 字面量
     * 
     * @param value 参数值
     * @return SQL 字面量字符串
     */
    private String formatParameterValue(Object value) {
        // NULL 处理
        if (value == null) {
            return "NULL";
        }

        // 数字和布尔值不加引号
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        // 日期时间类型
        try {
            String strValue;
            if (value instanceof Date) {
                strValue = DATE_FORMAT.get().format((Date) value);
            } else if (value instanceof LocalDateTime) {
                strValue = ((LocalDateTime) value).format(DATE_TIME_FORMATTER);
            } else if (value instanceof LocalDate) {
                strValue = ((LocalDate) value).format(DATE_FORMATTER);
            } else if (value instanceof LocalTime) {
                strValue = ((LocalTime) value).format(TIME_FORMATTER);
            } else if (value instanceof Enum) {
                strValue = ((Enum<?>) value).name();
            } else {
                strValue = value.toString();
            }
            // SQL 字符串转义：单引号 -> ''，反斜杠 -> \\
            return "'" + escapeSqlString(strValue) + "'";
        } catch (Exception e) {
            log.warn("格式化参数值失败: {}", value, e);
            return "'FORMAT_ERROR'";
        }
    }

    /**
     * 转义 SQL 字符串中的特殊字符
     * 主要处理单引号和反斜杠
     */
    private String escapeSqlString(String str) {
        if (str == null) {
            return "";
        }
        // 替换单引号为两个单引号（SQL 标准转义）
        // 注意：这里必须使用 replace 而非 replaceAll，避免正则问题
        return str.replace("\\", "\\\\").replace("'", "''");
    }

    /**
     * 压缩多余空白字符为单个空格
     * 避免使用 replaceAll 以防止参数中包含 $ 等特殊字符
     */
    private String compressWhitespace(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(sql.length());
        boolean lastWasWhitespace = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasWhitespace) {
                    result.append(' ');
                    lastWasWhitespace = true;
                }
            } else {
                result.append(c);
                lastWasWhitespace = false;
            }
        }
        
        return result.toString().trim();
    }

    /**
     * 替换 SQL 中的占位符 ?
     * 使用 StringBuilder 手动替换，避免正则表达式的特殊字符问题
     */
    private String replacePlaceholders(String sql, List<String> parameters) {
        StringBuilder result = new StringBuilder(sql.length() + parameters.size() * 20);
        int sqlIndex = 0;
        int paramIndex = 0;
        
        while (sqlIndex < sql.length() && paramIndex < parameters.size()) {
            int placeholderPos = sql.indexOf('?', sqlIndex);
            if (placeholderPos == -1) {
                // 没有更多占位符，添加剩余 SQL
                result.append(sql.substring(sqlIndex));
                break;
            }
            
            // 添加占位符前的内容
            result.append(sql, sqlIndex, placeholderPos);
            // 添加参数值
            result.append(parameters.get(paramIndex));
            
            sqlIndex = placeholderPos + 1;
            paramIndex++;
        }
        
        // 添加剩余 SQL（如果有）
        if (sqlIndex < sql.length()) {
            result.append(sql.substring(sqlIndex));
        }
        
        return result.toString();
    }

    /**
     * 打印 SQL 执行信息
     */
    private void printSql(String sqlId, String sql, long costTime,
                          Object result) {
        String resultInfo;
        if (result instanceof Collection<?> coll) {
            resultInfo = "返回行数: " + coll.size();
        } else if (result instanceof Number num) {
            resultInfo = "影响行数: " + num;
        } else if (result == null) {
            resultInfo = "无返回值";
        } else {
            resultInfo = "返回对象: " + result;
        }

        log.debug("\n=== SQL 执行 ===\n" +
                        "方法: {}\n" +
                        "SQL : {}\n" +
                        "耗时: {} ms\n" +
                        "{}",
                sqlId, sql, costTime, resultInfo);
    }

    /**
     * 统计字符出现次数
     */
    private int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    private static final class Holder {
        private static final SqlPrint INSTANCE = new SqlPrint();
    }
}