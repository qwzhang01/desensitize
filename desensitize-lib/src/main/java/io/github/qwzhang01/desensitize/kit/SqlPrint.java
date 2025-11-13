package io.github.qwzhang01.desensitize.kit;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SqlPrint {
    private static final Logger log = LoggerFactory.getLogger(SqlPrint.class);
    private final static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private SqlPrint() {
    }

    public static SqlPrint getInstance() {
        return SqlPrint.Holder.INSTANCE;
    }

    public void print(Configuration configuration, BoundSql boundSql, String sqlId, long startTime, Object result) {
        // 获取完整执行的SQL
        String sql = getSql(configuration, boundSql);
        // 打印SQL，执行时间，执行结果
        printSql(sqlId, sql, System.currentTimeMillis() - startTime, result);
    }

    /*** 获取完整的SQL语句 */
    private String getSql(Configuration configuration, BoundSql boundSql) {
        // 输入sql字符串空判断
        String sql = boundSql.getSql();
        if (StringUtil.isEmpty(sql)) {
            return "";
        }
        // 美化sql
        sql = sql.replaceAll("[\\s\n ]+", " ");
        // 填充占位符, 目前基本不用mybatis存储过程调用,故此处不做考虑
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        List<String> parameters = new ArrayList<>();
        if (parameterMappings != null) {
            MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    // 参数值
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    // 获取参数名称
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 获取参数值
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        // 如果是单个值则直接赋值
                        value = parameterObject;
                    } else {
                        value = metaObject == null ? null : metaObject.getValue(propertyName);
                    }
                    if (value instanceof Number) {
                        parameters.add(String.valueOf(value));
                    } else {
                        StringBuilder builder = new StringBuilder();
                        builder.append("'");
                        if (value instanceof Date) {
                            builder.append(new SimpleDateFormat(DATE_TIME_FORMAT).format((Date) value));
                        } else if (value instanceof LocalDateTime) {
                            builder.append(((LocalDateTime) value).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
                        } else if (value instanceof String) {
                            builder.append(value);
                        }
                        builder.append("'");
                        parameters.add(builder.toString());
                    }
                }
            }
        }
        for (String value : parameters) {
            sql = sql.replaceFirst("\\?", value);
        }
        return sql;
    }

    /*** 打印SQL、打印耗时 */
    private void printSql(String sqlId, String sql, long costTime, Object result) {
        if (result instanceof ArrayList resultList) {
            log.info("=== 执行SQL ===\n方法：{}\n执行SQL：{}\n耗时：{} ms\n返回：{} 行数据", sqlId, sql, costTime, resultList.size());
            return;
        }
        if (result instanceof Number row) {
            log.info("=== 执行SQL ===\n方法：{}\n执行SQL：{}\n耗时：{} ms\n影响：{} 行数据", sqlId, sql, costTime, row);
        }
    }

    private static final class Holder {
        private static final SqlPrint INSTANCE = new SqlPrint();
    }
}
