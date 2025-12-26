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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQL statement printing utility for debugging and monitoring.
 *
 * <p>This utility formats and prints SQL statements with actual parameter
 * values replaced, along with execution time and result information.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Replaces placeholders with actual values</li>
 *   <li>Supports various data types (String, Number, Date, LocalDateTime)</li>
 *   <li>Calculates and displays execution time</li>
 *   <li>Shows affected/returned row counts</li>
 *   <li>Thread-safe singleton pattern</li>
 * </ul>
 *
 * @author avinzhang
 */
public class SqlPrint {
    private static final Logger log = LoggerFactory.getLogger(SqlPrint.class);
    private final static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private SqlPrint() {
    }

    public static SqlPrint getInstance() {
        return SqlPrint.Holder.INSTANCE;
    }

    public void print(Configuration configuration, BoundSql boundSql,
                      String sqlId, long startTime, Object result) {
        // Get complete executable SQL
        String sql = getSql(configuration, boundSql);
        // Print SQL, execution time, and result
        printSql(sqlId, sql, System.currentTimeMillis() - startTime, result);
    }

    /**
     * Constructs complete SQL statement with parameter values.
     */
    private String getSql(Configuration configuration, BoundSql boundSql) {
        // Check for null/empty SQL string
        String sql = boundSql.getSql();
        if (StringUtil.isEmpty(sql)) {
            return "";
        }
        // Format SQL (normalize whitespace)
        sql = sql.replaceAll("[\\s\n ]+", " ");
        // Fill placeholders (stored procedures are not considered here)
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings =
                boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry =
                configuration.getTypeHandlerRegistry();
        List<String> parameters = new ArrayList<>();
        if (parameterMappings != null) {
            MetaObject metaObject = parameterObject == null ? null :
                    configuration.newMetaObject(parameterObject);
            for (ParameterMapping parameterMapping : parameterMappings) {
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    // Parameter value
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    // Get parameter name
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        // Get parameter value
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        // If single value, assign directly
                        value = parameterObject;
                    } else {
                        value = metaObject == null ? null :
                                metaObject.getValue(propertyName);
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

    /**
     * Prints SQL statement with execution time and result information.
     */
    private void printSql(String sqlId, String sql, long costTime,
                          Object result) {
        if (result instanceof ArrayList resultList) {
            log.info("=== SQL Execution ===\nMethod: {}\nSQL: {}\nTime: {} ms\nReturned: {} rows",
                    sqlId, sql, costTime, resultList.size());
            return;
        }
        if (result instanceof Number row) {
            log.info("=== SQL Execution ===\nMethod: {}\nSQL: {}\nTime: {} ms\nAffected: {} rows",
                    sqlId, sql, costTime, row);
        }
    }

    private static final class Holder {
        private static final SqlPrint INSTANCE = new SqlPrint();
    }
}
