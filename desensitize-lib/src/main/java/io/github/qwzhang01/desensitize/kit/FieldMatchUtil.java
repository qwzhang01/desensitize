package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.container.EncryptFieldTableContainer;
import io.github.qwzhang01.desensitize.domain.ParameterEncryptInfo;
import io.github.qwzhang01.desensitize.shield.EncryptionAlgo;
import io.github.qwzhang01.sql.tool.model.SqlParam;
import io.github.qwzhang01.sql.tool.model.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 字段匹配工具类
 *
 * @author avinzhang
 */
public final class FieldMatchUtil {

    private static final Logger log = LoggerFactory.getLogger(FieldMatchUtil.class);

    private FieldMatchUtil() {
        throw new UnsupportedOperationException("ParamUtil is a utility class and cannot be instantiated");
    }

    /**
     * 获取字段对应的加密算法类
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @return 加密算法类，如果不是加密字段则返回null
     */
    private static Class<? extends EncryptionAlgo> getEncryptAlgo(String tableName, String fieldName) {
        EncryptFieldTableContainer container = SpringContextUtil.getBean(EncryptFieldTableContainer.class);

        // 尝试多种命名格式
        String[] variants = {
                fieldName,
                StringUtil.camelToUnderscore(fieldName),
                StringUtil.underscoreToCamel(fieldName)
        };

        for (String variant : variants) {
            if (container.isEncrypt(tableName, variant)) {
                return container.getAlgo(tableName, variant);
            }
        }

        return null;
    }

    /**
     * 创建加密信息对象
     *
     * @param tableName 表名
     * @param fieldName 字段名
     * @param value     字段值
     * @return 加密信息对象，如果不是加密字段则返回null
     */
    public static ParameterEncryptInfo createEncryptInfo(String tableName, String fieldName, String value) {
        Class<? extends EncryptionAlgo> algoClass = getEncryptAlgo(tableName, fieldName);
        if (algoClass == null) {
            return null;
        }

        ParameterEncryptInfo encryptInfo = new ParameterEncryptInfo();
        encryptInfo.setTableName(tableName);
        encryptInfo.setFieldName(fieldName);
        encryptInfo.setOriginalValue(value);
        encryptInfo.setAlgoClass(algoClass);

        return encryptInfo;
    }

    /**
     * 匹配参数到表字段
     *
     * @param paramName   参数名
     * @param paramValue  参数值
     * @param sqlAnalysis SQL分析信息
     * @return 加密信息对象，如果不匹配则返回null
     */
    public static ParameterEncryptInfo matchParameterToTableField(String paramName, String paramValue,
                                                                  List<SqlParam> sqlAnalysis, List<SqlTable> tables) {
        // 清理参数名
        String cleanParamName = StringUtil.cleanParameterName(paramName);

        // 遍历所有表，检查是否有匹配的加密字段
        for (SqlTable tableInfo : tables) {
            String tableName = tableInfo.getName();

            ParameterEncryptInfo encryptInfo = createEncryptInfo(tableName, cleanParamName, paramValue);
            if (encryptInfo != null) {
                log.debug("直接匹配到加密字段: 表[{}] 字段[{}]", tableName, cleanParamName);
                return encryptInfo;
            }
        }

        // 从 SQL 条件中匹配
        for (SqlParam condition : sqlAnalysis) {
            String columnName = condition.getColumn();

            if (isFieldNameMatch(cleanParamName, columnName)) {
                // 找到匹配的字段，检查哪个表包含这个加密字段
                for (SqlTable tableInfo : tables) {
                    String tableName = tableInfo.getName();
                    ParameterEncryptInfo encryptInfo = createEncryptInfo(tableName, columnName, paramValue);
                    if (encryptInfo != null) {
                        log.debug("通过SQL条件匹配到加密字段: 表[{}] 字段[{}]", tableName, columnName);
                        return encryptInfo;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查字段名是否匹配（支持多种命名格式）
     *
     * @param paramName  参数名
     * @param columnName 列名
     * @return 是否匹配
     */
    private static boolean isFieldNameMatch(String paramName, String columnName) {
        if (paramName == null || columnName == null) {
            return false;
        }

        return paramName.equalsIgnoreCase(columnName) ||
                StringUtil.camelToUnderscore(paramName).equalsIgnoreCase(columnName) ||
                StringUtil.underscoreToCamel(paramName).equalsIgnoreCase(columnName);
    }
}