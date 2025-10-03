package io.github.qwzhang01.desensitize.scope;

/**
 * 数据权限策略
 *
 * @author avinzhang
 */
public interface DataScopeStrategy {
    /**
     * 数据权限设置 join 表信息
     */
    String join();

    /**
     * 数据权限设置 Where 条件
     */
    String where();
}