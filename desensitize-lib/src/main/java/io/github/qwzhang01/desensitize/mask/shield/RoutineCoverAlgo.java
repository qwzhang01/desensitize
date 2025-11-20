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


package io.github.qwzhang01.desensitize.mask.shield;

import java.util.regex.Pattern;

/**
 * Routine data masking algorithm implementation.
 *
 * @author avinzhang
 */
public class RoutineCoverAlgo implements CoverAlgo {

    private static final String MASK_CHAR = "*";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{18}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]+$");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("^[a-zA-Z]+$");

    /**
     * Default masking implementation
     */
    @Override
    public String mask(String content) {
        return "*****";
    }

    /**
     * Mask phone number (e.g., "138****5678")
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }

        phone = phone.trim();

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone;
        }

        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * Mask ID card number
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.trim().isEmpty()) {
            return idCard;
        }

        idCard = idCard.trim();

        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard;
        }

        if (idCard.length() == 18) {
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        } else if (idCard.length() == 15) {
            return idCard.substring(0, 6) + "******" + idCard.substring(12);
        }

        return idCard;
    }

    /**
     * Mask email address (e.g., "e****e@gmail.com")
     */
    public String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }

        email = email.trim();

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= 2) {
            return username.charAt(0) + MASK_CHAR + domain;
        } else {
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
     * Mask Chinese names (e.g., "张*" or "张*丰")
     */
    public String maskChineseName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        if (!CHINESE_PATTERN.matcher(name).matches()) {
            return name;
        }

        if (name.length() == 1) {
            return name;
        } else if (name.length() == 2) {
            return name.charAt(0) + MASK_CHAR;
        } else {
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
     * Mask English names (e.g., "J**n" or "S***h")
     */
    public String maskEnglishName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        name = name.trim();

        if (!ENGLISH_PATTERN.matcher(name).matches()) {
            return name;
        }

        if (name.length() <= 2) {
            return name.charAt(0) + MASK_CHAR;
        } else {
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