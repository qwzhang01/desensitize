package io.github.qwzhang01.desensitize.kit;

/**
 * Utility class for string operations commonly used in desensitization.
 *
 * <p>This class provides static utility methods for:</p>
 * <ul>
 *   <li>String validation (empty checks)</li>
 *   <li>Naming convention conversions (camelCase ↔ snake_case)</li>
 *   <li>Parameter name extraction and cleanup</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> All methods are static and stateless,
 * making this class thread-safe.</p>
 *
 * <p><strong>Design Pattern:</strong> Utility class pattern with private
 * constructor to prevent instantiation.</p>
 *
 * @author avinzhang
 */
public final class StringUtil {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private StringUtil() {
        throw new UnsupportedOperationException("StringUtil is a utility " +
                "class and cannot be instantiated");
    }

    /**
     * Converts camelCase string to snake_case (underscore separated).
     *
     * <p>This method converts uppercase letters to lowercase and inserts
     * underscores
     * before them using a regex pattern for efficiency.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>
     * StringUtil.camelToUnderscore("userName")     = "user_name"
     * StringUtil.camelToUnderscore("phoneNumber")  = "phone_number"
     * StringUtil.camelToUnderscore("ID")           = "id"
     * StringUtil.camelToUnderscore(null)           = null
     * StringUtil.camelToUnderscore("")             = ""
     * </pre>
     *
     * @param camelCase the camelCase string to convert
     * @return the snake_case string, or null/empty if input is null/empty
     */
    public static String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        // Regex pattern: match lowercase followed by uppercase, insert
        // underscore between them
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Converts snake_case string to camelCase.
     *
     * <p>This method removes underscores and capitalizes the character
     * following each underscore. The first character is lowercased.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>
     * StringUtil.underscoreToCamel("user_name")     = "userName"
     * StringUtil.underscoreToCamel("phone_number")  = "phoneNumber"
     * StringUtil.underscoreToCamel("_id")           = "id"
     * StringUtil.underscoreToCamel(null)            = null
     * StringUtil.underscoreToCamel("")              = ""
     * </pre>
     *
     * @param underscore the snake_case string to convert
     * @return the camelCase string, or null/empty if input is null/empty
     */
    public static String underscoreToCamel(String underscore) {
        if (underscore == null || underscore.isEmpty()) {
            return underscore;
        }

        StringBuilder result = new StringBuilder(underscore.length());
        boolean capitalizeNext = false;

        for (char c : underscore.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) :
                        Character.toLowerCase(c));
                capitalizeNext = false;
            }
        }

        return result.toString();
    }

    /**
     * Cleans a parameter name by removing common MyBatis parameter prefixes.
     *
     * <p>MyBatis often adds prefixes like "param." or "arg." to parameter
     * names.
     * This method removes these prefixes to get the actual field name.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>
     * StringUtil.cleanParameterName("param.userId")  = "userId"
     * StringUtil.cleanParameterName("arg.phoneNo")   = "phoneNo"
     * StringUtil.cleanParameterName("fieldName")     = "fieldName"
     * StringUtil.cleanParameterName(null)            = null
     * </pre>
     *
     * @param paramName the parameter name to clean
     * @return the cleaned parameter name without prefixes
     */
    public static String cleanParameterName(String paramName) {
        if (paramName == null) {
            return null;
        }

        String cleaned = paramName;

        // Remove MyBatis parameter prefixes
        if (cleaned.startsWith("param.")) {
            cleaned = cleaned.substring(6);
        } else if (cleaned.startsWith("arg.")) {
            cleaned = cleaned.substring(4);
        }

        return cleaned;
    }

    /**
     * Extracts the field name from a potentially nested property path.
     *
     * <p>This method extracts the portion after the last dot, which typically
     * represents the actual field name in nested objects.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>
     * StringUtil.extractFieldName("user.address.city")  = "city"
     * StringUtil.extractFieldName("userParam.phoneNo")  = "phoneNo"
     * StringUtil.extractFieldName("fieldName")          = "fieldName"
     * StringUtil.extractFieldName("field.")             = "field."
     * StringUtil.extractFieldName(null)                 = null
     * </pre>
     *
     * @param propertyPath the property path to extract from (may contain
     *                     dots for nested properties)
     * @return the extracted field name, or the original string if no dot is
     * found
     */
    public static String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return propertyPath;
        }

        // Find the last dot and extract the field name after it
        int lastDotIndex = propertyPath.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }

        return propertyPath;
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * <p><strong>Examples:</strong></p>
     * <pre>
     * StringUtil.isEmpty(null)      = true
     * StringUtil.isEmpty("")        = true
     * StringUtil.isEmpty(" ")       = true
     * StringUtil.isEmpty("  ")      = true
     * StringUtil.isEmpty("hello")   = false
     * StringUtil.isEmpty(" hi ")    = false
     * </pre>
     *
     * @param str the string to check
     * @return {@code true} if the string is null, empty, or whitespace only;
     * {@code false} otherwise
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String clearSqlTip(String sql) {
        if (isEmpty(sql)) {
            return sql;
        }
        return sql.trim().replaceAll("`", "");
    }

}