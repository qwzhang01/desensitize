package io.github.qwzhang01.desensitize.shield;

import java.util.regex.Pattern;

/**
 * 默认脱敏算法实现
 *
 * @author qwzhang01
 */
public class RoutineCoverAlgo implements CoverAlgo {

    private static final String MASK_CHAR = "*";

    // 手机号正则：1开头的11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 身份证号正则：18位或15位
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{18}$");

    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    // 中文字符正则
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");

    // 英文字符正则
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    /**
     * 默认脱敏算法
     *
     * @param content
     * @return
     */
    @Override
    public String mask(String content) {
        return "*****";
    }

    /**
     * 手机号脱敏 - 中间4位替换为*
     * 例: 13812345678 -> 1381****5678
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        phone = phone.trim();

        // 验证手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone; // 格式不正确，返回原值
        }

        // 中间4位替换为*：1381****5678
        return phone.substring(0, 4) + "****" + phone.substring(7);
    }

    /**
     * 身份证号脱敏 - 中间出生年月日替换为*
     * 例: 110101199001011234 -> 110101********1234
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        idCard = idCard.trim();

        // 验证身份证号格式
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard; // 格式不正确，返回原值
        }

        if (idCard.length() == 18) {
            // 18位身份证：前6位 + 8个* + 后4位
            // 110101199001011234 -> 110101********1234
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        } else if (idCard.length() == 15) {
            // 15位身份证：前6位 + 6个* + 后3位
            // 110101900101123 -> 110101******123
            return idCard.substring(0, 6) + "******" + idCard.substring(12);
        }

        return idCard;
    }

    /**
     * 邮箱脱敏 - 用户名保留开头结尾，中间替换为*
     * 例: example@gmail.com -> e****e@gmail.com
     */
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        email = email.trim();

        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email; // 格式不正确，返回原值
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            // 用户名太短，只保留第一个字符
            return username.charAt(0) + MASK_CHAR + domain;
        } else {
            // 保留开头结尾，中间替换为*
            // example@gmail.com -> e****e@gmail.com
            StringBuilder masked = new StringBuilder();
            masked.append(username.charAt(0));
            for (int i = 1; i < username.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(username.charAt(username.length() - 1));
            masked.append(domain);
            return masked.toString();
        }
    }

    /**
     * 中文姓名脱敏
     * 2位：只保留姓，名字替换为* (例: 张三 -> 张*)
     * 2位以上：保留开头结尾，中间替换为* (例: 张三丰 -> 张*丰)
     */
    public String maskChineseName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        // 验证是否为中文字符
        if (!CHINESE_PATTERN.matcher(name).matches()) {
            return name; // 不是中文，返回原值
        }

        if (name.length() == 1) {
            // 单字名，返回原值
            return name;
        } else if (name.length() == 2) {
            // 2位：只保留姓，名字替换为*
            // 张三 -> 张*
            return name.charAt(0) + MASK_CHAR;
        } else {
            // 2位以上：保留开头结尾，中间替换为*
            // 张三丰 -> 张*丰
            // 欧阳修文 -> 欧***文
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            for (int i = 1; i < name.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(name.charAt(name.length() - 1));
            return masked.toString();
        }
    }

    /**
     * 英文姓名脱敏 - 保留开头结尾字母，中间替换为*
     * 例: John -> J**n, Smith -> S***h
     */
    public String maskEnglishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        // 验证是否为英文字符
        if (!ENGLISH_PATTERN.matcher(name).matches()) {
            return name; // 不是英文，返回原值
        }

        if (name.length() <= 2) {
            // 名字太短，只保留第一个字符
            return name.charAt(0) + MASK_CHAR;
        } else {
            // 保留开头结尾字母，中间替换为*
            // John -> J**n
            // Smith -> S***h
            StringBuilder masked = new StringBuilder();
            masked.append(name.charAt(0));
            for (int i = 1; i < name.length() - 1; i++) {
                masked.append(MASK_CHAR);
            }
            masked.append(name.charAt(name.length() - 1));
            return masked.toString();
        }
    }
}