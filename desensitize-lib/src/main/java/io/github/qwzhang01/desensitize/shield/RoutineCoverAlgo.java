/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.desensitize.shield;

import java.util.regex.Pattern;

/**
 * Routine data masking algorithm implementation.
 * Provides comprehensive masking strategies for various types of sensitive data
 * including phone numbers, ID cards, emails, and names in both Chinese and English.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class RoutineCoverAlgo implements CoverAlgo {

    /**
     * The character used for masking sensitive data
     */
    private static final String MASK_CHAR = "*";

    /**
     * Regular expression pattern for Chinese mobile phone numbers
     * Matches 11-digit numbers starting with 1 and second digit from 3-9
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * Regular expression pattern for Chinese ID card numbers
     * Matches both 15-digit and 18-digit ID card formats
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{18}$");

    /**
     * Regular expression pattern for email addresses
     * Matches standard email format with alphanumeric characters, dots, and hyphens
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    /**
     * Regular expression pattern for Chinese characters
     * Matches Unicode range for Chinese characters (U+4E00 to U+9FA5)
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");

    /**
     * Regular expression pattern for English characters
     * Matches only alphabetic characters (a-z, A-Z)
     */
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    /**
     * Default masking implementation that returns a fixed mask string.
     * This method serves as a fallback when specific masking logic is not available.
     *
     * @param content the content to be masked (not used in default implementation)
     * @return a fixed mask string "*****"
     */
    @Override
    public String mask(String content) {
        return "*****";
    }

    /**
     * Masks phone number by replacing middle 4 digits with asterisks.
     * Preserves the first 3 digits and last 4 digits for identification purposes.
     *
     * @param phone the phone number to be masked (e.g., "13812345678")
     * @return masked phone number (e.g., "138****5678") or original if invalid format
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        phone = phone.trim();

        // Validate phone number format
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone; // Return original value if format is incorrect
        }

        // Replace middle 4 digits with asterisks: 13812345678 -> 138****5678
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * Masks ID card number by replacing birth date portion with asterisks.
     * For 18-digit ID cards, masks 8 middle digits (birth date).
     * For 15-digit ID cards, masks 6 middle digits (birth date).
     *
     * @param idCard the ID card number to be masked
     * @return masked ID card number or original if invalid format
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        idCard = idCard.trim();

        // Validate ID card format
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard; // Return original value if format is incorrect
        }

        if (idCard.length() == 18) {
            // 18-digit ID card: first 6 digits + 8 asterisks + last 4 digits
            // Example: 110101199001011234 -> 110101********1234
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        } else if (idCard.length() == 15) {
            // 15-digit ID card: first 6 digits + 6 asterisks + last 3 digits
            // Example: 110101900101123 -> 110101******123
            return idCard.substring(0, 6) + "******" + idCard.substring(12);
        }

        return idCard;
    }

    /**
     * Masks email address by preserving first and last characters of username.
     * The domain part remains unchanged for functional purposes.
     *
     * @param email the email address to be masked (e.g., "example@gmail.com")
     * @return masked email address (e.g., "e****e@gmail.com") or original if invalid format
     */
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        email = email.trim();

        // Validate email format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email; // Return original value if format is incorrect
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            // Username too short, keep only first character
            return username.charAt(0) + MASK_CHAR + domain;
        } else {
            // Preserve first and last characters, mask middle part
            // Example: example@gmail.com -> e****e@gmail.com
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
     * Masks Chinese names according to traditional privacy protection rules.
     * For 2-character names: keeps family name, masks given name.
     * For names longer than 2 characters: keeps first and last characters, masks middle.
     *
     * @param name the Chinese name to be masked (e.g., "张三" or "张三丰")
     * @return masked Chinese name (e.g., "张*" or "张*丰") or original if not Chinese
     */
    public String maskChineseName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        // Validate if contains only Chinese characters
        if (!CHINESE_PATTERN.matcher(name).matches()) {
            return name; // Return original value if not Chinese
        }

        if (name.length() == 1) {
            // Single character name, return as is
            return name;
        } else if (name.length() == 2) {
            // 2-character name: keep family name, mask given name
            // Example: 张三 -> 张*
            return name.charAt(0) + MASK_CHAR;
        } else {
            // Names longer than 2 characters: keep first and last, mask middle
            // Examples: 张三丰 -> 张*丰, 欧阳修文 -> 欧**文
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
     * Masks English names by preserving first and last characters.
     * Replaces middle characters with asterisks for privacy protection.
     *
     * @param name the English name to be masked (e.g., "John" or "Smith")
     * @return masked English name (e.g., "J**n" or "S***h") or original if not English
     */
    public String maskEnglishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        // Validate if contains only English characters
        if (!ENGLISH_PATTERN.matcher(name).matches()) {
            return name; // Return original value if not English
        }

        if (name.length() <= 2) {
            // Name too short, keep only first character
            return name.charAt(0) + MASK_CHAR;
        } else {
            // Preserve first and last characters, mask middle part
            // Examples: John -> J**n, Smith -> S***h
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