package com.qw.desensitize.strategy;

import io.github.qwzhang01.desensitize.scope.EmptyDataScopeStrategy;

public class WhereDataScopeStrategy extends EmptyDataScopeStrategy {


    @Override
    public String where() {
        return "user.idAvin = 100 and exists(select 1 from post where user.id = post.user_id)";
    }
}
