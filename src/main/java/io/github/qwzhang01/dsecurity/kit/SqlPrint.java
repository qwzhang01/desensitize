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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 改进版 SQL 打印工具类
 * 修复了原版的多项问题，提升了健壮性、准确性和可维护性
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

        // 统一空白字符为单个空格，便于阅读
        sql = sql.replaceAll("\\s+", " ").trim();

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

                // NULL 处理
                if (value == null) {
                    parameters.add("NULL");
                    continue;
                }

                // 数字和布尔值不加引号
                if (value instanceof Number || value instanceof Boolean) {
                    parameters.add(String.valueOf(value));
                    continue;
                }

                // 其他类型统一加单引号并转义内部单引号
                StringBuilder sb = new StringBuilder("'");
                try {
                    String strValue;
                    if (value instanceof Date) {
                        strValue = DATE_FORMAT.get().format((Date) value);
                    } else if (value instanceof LocalDateTime) {
                        strValue =
                                ((LocalDateTime) value).format(DATE_TIME_FORMATTER);
                    } else {
                        strValue = value.toString();
                    }
                    // 单引号转义
                    strValue = strValue.replace("'", "''");
                    sb.append(strValue);
                } catch (Exception e) {
                    sb.append("FORMAT_ERROR");
                }
                sb.append("'");
                parameters.add(sb.toString());
            }
        }

        // 校验占位符数量（可选，预防极端情况）
        int placeholderCount = countChar(sql, '?');
        if (placeholderCount != parameters.size()) {
            log.warn("SQL 占位符数量({}) 与参数数量({}) 不匹配，Method: {}",
                    placeholderCount, parameters.size(),
                    boundSql.getParameterObject());
        }

        // 逐个替换 ?
        StringBuilder sqlBuilder = new StringBuilder(sql);
        int offset = 0;
        for (String param : parameters) {
            int idx = sqlBuilder.indexOf("?", offset);
            if (idx == -1) {
                break; // 防止参数多于占位符
            }
            sqlBuilder.replace(idx, idx + 1, param);
            offset = idx + param.length();
        }

        return sqlBuilder.toString();
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