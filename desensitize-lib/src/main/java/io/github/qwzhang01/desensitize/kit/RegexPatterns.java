package io.github.qwzhang01.desensitize.kit;

import java.util.regex.Pattern;

/**
 * 正则表达式模式常量类
 *
 * @author avinzhang
 */
public final class RegexPatterns {

    /**
     * QueryWrapper 等值条件模式 - field_name = #{ew.paramNameValuePairs.MPGENVAL1}
     */
    public static final Pattern QW_EQUAL_PATTERN = Pattern.compile(
            "(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );

    // ========== QueryWrapper 参数解析相关正则表达式 ==========
    /**
     * QueryWrapper LIKE 条件模式 - field_name LIKE #{ew.paramNameValuePairs.MPGENVAL1}
     */
    public static final Pattern QW_LIKE_PATTERN = Pattern.compile(
            "(\\w+)\\s+LIKE\\s+#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );
    /**
     * QueryWrapper IN 条件模式 - field_name IN (#{ew.paramNameValuePairs.MPGENVAL1}, ...)
     */
    public static final Pattern QW_IN_PATTERN = Pattern.compile(
            "(\\w+)\\s+IN\\s*\\([^)]*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}"
    );

    private RegexPatterns() {
        // 常量类不允许实例化
    }
}