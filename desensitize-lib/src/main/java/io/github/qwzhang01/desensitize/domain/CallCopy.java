package io.github.qwzhang01.desensitize.domain;

/**
 * 参数化方法
 * 对象拷贝
 *
 * @author avinzhang
 */
@FunctionalInterface
public interface CallCopy<S, T> {
    /**
     * 正常拷贝结束后执行的其他字段转换逻辑
     *
     * @param source 数据源类
     * @param target 目标数据类
     */
    void call(S source, T target);
}
