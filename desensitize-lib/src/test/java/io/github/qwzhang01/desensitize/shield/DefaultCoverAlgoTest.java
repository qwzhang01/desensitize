package io.github.qwzhang01.desensitize.shield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 默认脱敏算法测试类
 */
public class DefaultCoverAlgoTest {

    private RoutineCoverAlgo coverAlgo;

    @BeforeEach
    void setUp() {
        coverAlgo = new RoutineCoverAlgo();
    }

    @Test
    void testMaskPhone() {
        // 正常手机号
        assertEquals("138****5678", coverAlgo.maskPhone("13812345678"));
        assertEquals("150****9999", coverAlgo.maskPhone("15012349999"));

        // 边界情况
        assertNull(coverAlgo.maskPhone(null));
        assertEquals("", coverAlgo.maskPhone(""));
        assertEquals("   ", coverAlgo.maskPhone("   "));

        // 格式错误的手机号，应返回原值
        assertEquals("12345678901", coverAlgo.maskPhone("12345678901")); // 不是1开头
        assertEquals("1381234567", coverAlgo.maskPhone("1381234567"));   // 位数不够
        assertEquals("138123456789", coverAlgo.maskPhone("138123456789")); // 位数过多
    }

    @Test
    void testMaskIdCard() {
        // 18位身份证
        assertEquals("110101********1234", coverAlgo.maskIdCard("110101199001011234"));
        assertEquals("320102********5678", coverAlgo.maskIdCard("320102198505125678"));

        // 15位身份证
        assertEquals("110101******123", coverAlgo.maskIdCard("110101900101123"));

        // 边界情况
        assertNull(coverAlgo.maskIdCard(null));
        assertEquals("", coverAlgo.maskIdCard(""));

        // 格式错误的身份证，应返回原值
        assertEquals("1234567890", coverAlgo.maskIdCard("1234567890")); // 位数不对
        assertEquals("11010119900101123a", coverAlgo.maskIdCard("11010119900101123a")); // 包含字母
    }

    @Test
    void testMaskEmail() {
        // 正常邮箱
        assertEquals("e*****e@gmail.com", coverAlgo.maskEmail("example@gmail.com"));
        assertEquals("t**t@qq.com", coverAlgo.maskEmail("test@qq.com"));
        assertEquals("a*c@163.com", coverAlgo.maskEmail("abc@163.com"));

        // 短用户名
        assertEquals("a*@test.com", coverAlgo.maskEmail("ab@test.com"));
        assertEquals("a*@test.com", coverAlgo.maskEmail("a@test.com"));

        // 边界情况
        assertNull(coverAlgo.maskEmail(null));
        assertEquals("", coverAlgo.maskEmail(""));

        // 格式错误的邮箱，应返回原值
        assertEquals("notanemail", coverAlgo.maskEmail("notanemail"));
        assertEquals("test@", coverAlgo.maskEmail("test@"));
        assertEquals("@gmail.com", coverAlgo.maskEmail("@gmail.com"));
    }

    @Test
    void testMaskChineseName() {
        // 2位中文名
        assertEquals("张*", coverAlgo.maskChineseName("张三"));
        assertEquals("李*", coverAlgo.maskChineseName("李四"));

        // 3位中文名
        assertEquals("张*丰", coverAlgo.maskChineseName("张三丰"));
        assertEquals("诸*亮", coverAlgo.maskChineseName("诸葛亮"));

        // 4位中文名
        assertEquals("欧**文", coverAlgo.maskChineseName("欧阳修文"));
        assertEquals("司**光", coverAlgo.maskChineseName("司马相光"));

        // 单字名
        assertEquals("王", coverAlgo.maskChineseName("王"));

        // 边界情况
        assertNull(coverAlgo.maskChineseName(null));
        assertEquals("", coverAlgo.maskChineseName(""));

        // 非中文名，应返回原值
        assertEquals("John", coverAlgo.maskChineseName("John"));
        assertEquals("张三123", coverAlgo.maskChineseName("张三123"));
    }

    @Test
    void testMaskEnglishName() {
        // 正常英文名
        assertEquals("J**n", coverAlgo.maskEnglishName("John"));
        assertEquals("S***h", coverAlgo.maskEnglishName("Smith"));
        assertEquals("A***e", coverAlgo.maskEnglishName("Alice"));

        // 短名字
        assertEquals("A*", coverAlgo.maskEnglishName("Ab"));
        assertEquals("A*", coverAlgo.maskEnglishName("A"));

        // 边界情况
        assertNull(coverAlgo.maskEnglishName(null));
        assertEquals("", coverAlgo.maskEnglishName(""));

        // 非英文名，应返回原值
        assertEquals("张三", coverAlgo.maskEnglishName("张三"));
        assertEquals("John123", coverAlgo.maskEnglishName("John123"));
        assertEquals("John-Smith", coverAlgo.maskEnglishName("John-Smith"));
    }
}