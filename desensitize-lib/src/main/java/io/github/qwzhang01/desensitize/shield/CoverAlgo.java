package io.github.qwzhang01.desensitize.shield;

/**
 * 脱敏算法接口
 * 用于对敏感数据进行脱敏处理
 */
public interface CoverAlgo {
    /**
     * 对敏感数据进行脱敏处理
     *
     * @param content 待脱敏的敏感数据
     * @return 脱敏后的数据
     */
    String mask(String content);
}
