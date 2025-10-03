package io.github.qwzhang01.desensitize.kit;

/**
 * 字符串工具类
 *
 * @author avinzhang
 */
public final class StringUtil {

    private StringUtil() {
        // 工具类不允许实例化
    }

    /**
     * 驼峰转下划线
     *
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 下划线转驼峰
     *
     * @param underscore 下划线命名字符串
     * @return 驼峰命名字符串
     */
    public static String underscoreToCamel(String underscore) {
        if (underscore == null || underscore.isEmpty()) {
            return underscore;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;

        for (char c : underscore.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            }
        }

        return result.toString();
    }

    /**
     * 清理参数名，移除常见的前缀
     *
     * @param paramName 参数名
     * @return 清理后的参数名
     */
    public static String cleanParameterName(String paramName) {
        if (paramName == null) {
            return null;
        }

        String cleaned = paramName;
        if (cleaned.startsWith("param.")) {
            cleaned = cleaned.substring(6);
        }
        if (cleaned.startsWith("arg.")) {
            cleaned = cleaned.substring(4);
        }

        return cleaned;
    }

    /**
     * 从嵌套属性路径中提取字段名
     * 例如：userParam.phoneNo -> phoneNo
     *
     * @param propertyPath 属性路径
     * @return 字段名
     */
    public static String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return propertyPath;
        }

        // 如果包含点号，取最后一部分作为字段名
        int lastDotIndex = propertyPath.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }

        return propertyPath;
    }

    /**
     * 检查字符串是否为空或null
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}