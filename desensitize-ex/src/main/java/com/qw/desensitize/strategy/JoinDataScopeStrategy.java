package com.qw.desensitize.strategy;

import io.github.qwzhang01.desensitize.scope.EmptyDataScopeStrategy;

public class JoinDataScopeStrategy extends EmptyDataScopeStrategy {

    @Override
    public String join() {
        return "LEFT JOIN belong_org p ON user.id = p.objectId";
    }
}
