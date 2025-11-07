package com.qw.desensitize.strategy;

import io.github.qwzhang01.desensitize.scope.EmptyDataScopeStrategy;

public class WhereDataScopeStrategy extends EmptyDataScopeStrategy<Long> {


    @Override
    public String where() {
        return "user.name = '100' and exists(select 1 from belong_org post where user.id = post.user_id)";
    }
}
