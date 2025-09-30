package io.github.qwzhang01.desensitize.shield;

/**
 * 默认脱敏算法实现
 *
 * @author qwzhang01
 */
public class EmailCoverAlgo extends RoutineCoverAlgo {
    @Override
    public String mask(String content) {
        return super.maskEmail(content);
    }
}