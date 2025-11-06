package io.github.qwzhang01.desensitize.scope;

import java.util.List;

/**
 * 数据权限策略接口
 * <p>
 * 定义数据权限过滤的核心方法，用于在SQL查询中添加权限控制条件
 *
 * @param <T> 权限数据类型，通常为权限ID或权限对象
 * @author avinzhang
 */
public interface DataScopeStrategy<T> {
    /**
     * 数据权限设置 join 表信息
     * <p>
     * 重要，关联表，如果不在脚本中，需要用表全名且不带别名
     *
     * @return JOIN子句字符串，如果不需要关联表则返回空字符串
     */
    String join();

    /**
     * 数据权限设置 Where 条件
     * <p>
     * 重要，子查询或exists条件，如果存在表不在脚本中，需要用表全名
     *
     * @return WHERE条件字符串，如果不需要额外条件则返回空字符串
     */
    String where();

    /**
     * 验证和设置有效的权限数据
     * <p>
     * 在执行SQL查询前调用，用于处理和验证权限数据
     *
     * @param validRights 有效的权限数据列表
     */
    void validDs(List<T> validRights);

    /**
     * 验证和设置有效的权限数据
     * <p>
     * 在执行SQL查询前调用，用于处理和验证权限数据
     * <p>
     * validRights  withoutRights 是或的关系，只要有一个校验通过，则通过
     *
     * @param validRights   有效的权限数据列表
     * @param withoutRights 白名单，无需按照权限校验
     */
    void validDs(List<T> validRights, List<T> withoutRights);
}