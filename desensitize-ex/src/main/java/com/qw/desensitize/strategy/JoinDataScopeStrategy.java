package com.qw.desensitize.strategy;

import io.github.qwzhang01.desensitize.scope.EmptyDataScopeStrategy;

public class JoinDataScopeStrategy extends EmptyDataScopeStrategy {

    @Override
    public String join() {
        return "LEFT JOIN t_user u ON u.id = post.user_id";
    }
}
