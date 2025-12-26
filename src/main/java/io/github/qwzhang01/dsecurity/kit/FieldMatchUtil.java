package io.github.qwzhang01.dsecurity.kit;

import io.github.qwzhang01.dsecurity.domain.ParameterEncryptInfo;
import io.github.qwzhang01.dsecurity.encrypt.container.EncryptFieldTableContainer;
import io.github.qwzhang01.dsecurity.encrypt.shield.EncryptionAlgo;
import io.github.qwzhang01.sql.tool.model.SqlParam;
import io.github.qwzhang01.sql.tool.model.SqlTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Field matching utility for encryption field identification.
 *
 * <p>This utility class provides methods to:</p>
 * <ul>
 *   <li>Match SQL parameters to database table fields</li>
 *   <li>Identify fields that require encryption</li>
 *   <li>Retrieve appropriate encryption algorithms for fields</li>
 *   <li>Support multiple naming conventions (camelCase and snake_case)</li>
 * </ul>
 *
 * @author avinzhang
 */
public final class FieldMatchUtil {

    private static final Logger log =
            LoggerFactory.getLogger(FieldMatchUtil.class);

    private FieldMatchUtil() {
        throw new UnsupportedOperationException("ParamUtil is a utility class" +
                " and cannot be instantiated");
    }

    /**
     * Retrieves the encryption algorithm class for a specific table field.
     *
     * <p>This method tries multiple naming format variants to match the field:</p>
     * <ul>
     *   <li>Original field name</li>
     *   <li>camelCase to snake_case conversion</li>
     *   <li>snake_case to camelCase conversion</li>
     * </ul>
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @return the encryption algorithm class, or null if field is not encrypted
     */
    private static Class<? extends EncryptionAlgo> getEncryptAlgo(String tableName, String fieldName) {
        EncryptFieldTableContainer container =
                SpringContextUtil.getBean(EncryptFieldTableContainer.class);

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
     * Creates encryption information object for a field.
     *
     * @param tableName the table name
     * @param fieldName the field name
     * @param value     the field value to encrypt
     * @return encryption information object, or null if field is not encrypted
     */
    public static ParameterEncryptInfo createEncryptInfo(String tableName,
                                                         String fieldName,
                                                         String value) {
        Class<? extends EncryptionAlgo> algoClass = getEncryptAlgo(tableName,
                fieldName);
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
     * Matches a parameter to a table field for encryption.
     *
     * <p>The matching process includes:</p>
     * <ol>
     *   <li>Direct field name matching across all tables</li>
     *   <li>SQL condition analysis to find field references</li>
     *   <li>Multiple naming convention support</li>
     * </ol>
     *
     * @param paramName   the parameter name
     * @param paramValue  the parameter value
     * @param sqlAnalysis SQL analysis information
     * @param tables      list of tables in the SQL
     * @return encryption information if match found, null otherwise
     */
    public static ParameterEncryptInfo matchParameterToTableField(String paramName, String paramValue,
                                                                  List<SqlParam> sqlAnalysis, List<SqlTable> tables) {
        // 清理参数名
        String cleanParamName = StringUtil.cleanParameterName(paramName);

        // 遍历所有表，检查是否有匹配的加密字段
        for (SqlTable tableInfo : tables) {
            String tableName = tableInfo.getName();

            ParameterEncryptInfo encryptInfo = createEncryptInfo(tableName,
                    cleanParamName, paramValue);
            if (encryptInfo != null) {
                log.debug("Direct match found for encrypted field: table[{}] field[{}]", tableName, cleanParamName);
                return encryptInfo;
            }
        }

        // Match from SQL conditions
        for (SqlParam condition : sqlAnalysis) {
            String columnName = condition.getColumn();

            if (isFieldNameMatch(cleanParamName, columnName)) {
                // Found matching field, check which table contains this encrypted field
                for (SqlTable tableInfo : tables) {
                    String tableName = tableInfo.getName();
                    ParameterEncryptInfo encryptInfo =
                            createEncryptInfo(tableName, columnName,
                                    paramValue);
                    if (encryptInfo != null) {
                        log.debug("Matched encrypted field via SQL condition: table[{}] field[{}]", tableName,
                                columnName);
                        return encryptInfo;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if field name matches (supports multiple naming formats).
     *
     * @param paramName  the parameter name
     * @param columnName the column name
     * @return true if names match
     */
    private static boolean isFieldNameMatch(String paramName,
                                            String columnName) {
        if (paramName == null || columnName == null) {
            return false;
        }

        return paramName.equalsIgnoreCase(columnName) ||
                StringUtil.camelToUnderscore(paramName).equalsIgnoreCase(columnName) ||
                StringUtil.underscoreToCamel(paramName).equalsIgnoreCase(columnName);
    }
}