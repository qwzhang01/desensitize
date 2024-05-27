package com.qw.desensitize;

import org.junit.jupiter.api.Test;

import static com.qw.desensitize.kit.DesKit.*;

class DesensitizeApplicationTest {

    /**
     * 加密测试
     */
    @Test
    public void encryptTest() {
        String content = "我爱你";
        String encryptStr = encrypt(KEY, content);
        System.out.println(encryptStr);
    }

    /**
     * 解密测试
     */
    @Test
    public void decryptTest() {
        String content = "Xii999DE7LPx5io0awfOFw==";
        String decryptStr = decrypt(KEY, content);
        System.out.println(decryptStr);
    }
}
