package io.github.qwzhang01.desensitize.scope;

/**
 * 数据权限策略
 *
 * @author avinzhang
 */
public interface DataScopeStrategy {
    /**
     * 数据权限设置 join 表信息
     * <p>
     * 重要，关联表，如果不在脚本中，需要用表全名且不带别名
     */
    String join();

    /**
     * 数据权限设置 Where 条件
     * 重要，子查询或exists条件，如果存在表不在脚本中，需要用表全名
     */
    String where();
}