package com.qw.desensitize.strategy;

import io.github.qwzhang01.desensitize.scope.EmptyDataScopeStrategy;

public class WhereDataScopeStrategy extends EmptyDataScopeStrategy {


    @Override
    public String where() {

        return "u.idAvin = 100";
    }
}
