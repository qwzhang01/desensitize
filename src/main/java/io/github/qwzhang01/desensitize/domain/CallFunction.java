package io.github.qwzhang01.desensitize.domain;

/**
 * 参数化方法
 * 业务逻辑
 *
 * @author avinzhang
 */
@FunctionalInterface
public interface CallFunction {
    /**
     * 具体执行的逻辑
     *
     * @param param 参数
     * @return 是否成功执行
     */
    boolean call(String param);
}
