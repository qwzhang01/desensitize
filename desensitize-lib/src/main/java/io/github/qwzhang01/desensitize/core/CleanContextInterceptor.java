package io.github.qwzhang01.desensitize.core;


import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;

/**
 * 加密、解密、数据权限 context 清除
 *
 * @author avinzhang
 */
@Intercepts({@Signature(type = ParameterHandler.class, method = "execute", args = {PreparedStatement.class})})
public class CleanContextInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(CleanContextInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } finally {
            SqlRewriteContext.restore();
        }
    }
}
