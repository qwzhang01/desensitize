package io.github.qwzhang01.desensitize.scope;

import java.util.Collections;
import java.util.List;

/**
 * 默认策略
 * 什么也不做
 *
 * @author avinzhang
 */
public class DefaultDataScopeStrategy extends DataScopeStrategy {
    @Override
    protected void configJoin() {
        //do nothing
    }

    @Override
    protected List<Where> configWhere() {
        //do nothing
        return Collections.emptyList();
    }
}
