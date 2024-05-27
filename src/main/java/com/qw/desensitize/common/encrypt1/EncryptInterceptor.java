package com.qw.desensitize.common.encrypt1;

import com.qw.desensitize.kit.DesKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static com.qw.desensitize.kit.DesKit.KEY;


/**
 * Intercepts 拦截器
 * Signature拦截器类型设置
 * <p>
 * type 属性指定拦截器拦截的类StatementHandler 、ResultSetHandler、ParameterHandler，Executor
 * Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed) 处理增删改查
 * ParameterHandler (getParameterObject, setParameters) 设置预编译参数
 * ResultSetHandler (handleResultSets, handleOutputParameters) 处理结果
 * StatementHandler (prepare, parameterize, batch, update, query) 处理sql预编译，设置参数
 * <p>
 * method 拦截对应类的方法
 * <p>
 * args 被拦截方法的参数
 * <p>
 *
 * @author avinzhang
 */
@Slf4j
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class)})
public class EncryptInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // @Signature 指定了 type= parameterHandler 后，这里的 invocation.getTarget() 便是parameterHandler
        // 若指定ResultSetHandler ，这里则能强转为ResultSetHandler
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();

        MetaObject metaObject = MetaObject.forObject(parameterHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
        //sql语句类型：UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH、update,SqlCommandType是个enum
        String sqlCommandType = mappedStatement.getSqlCommandType().toString();
        if (!"INSERT".equals(sqlCommandType) && !"UPDATE".equals(sqlCommandType)) {
            return invocation.proceed();
        }

        // 获取参数对象，即mapper中paramsType的实例
        Field paramsFiled = parameterHandler.getClass().getDeclaredField("parameterObject");
        // 将此对象的 accessible 标志设置为指示的布尔值。值为 true 则指示反射的对象在使用时应该取消 Java 语言访问检查。
        paramsFiled.setAccessible(true);
        // 取出实例
        Object parameterObject = paramsFiled.get(parameterHandler);
        if (parameterObject != null) {
            Class<?> parameterObjectClass = null;
            if (parameterObject instanceof MapperMethod.ParamMap) {
                // 更新操作被拦截
                Map paramMap = (Map) parameterObject;
                if (paramMap.containsKey("et")) {
                    parameterObject = paramMap.get("et");
                    if (parameterObject != null) {
                        parameterObjectClass = parameterObject.getClass();
                    }
                }
            } else {
                parameterObjectClass = parameterObject.getClass();
            }
            if (parameterObjectClass != null) {
                // 校验该实例的类是否被@SensitiveData所注解
                EncryptObj sensitiveData = AnnotationUtils.findAnnotation(parameterObjectClass, EncryptObj.class);
                if (Objects.nonNull(sensitiveData)) {
                    //取出当前类的所有字段，传入加密方法
                    Field[] declaredFields = parameterObjectClass.getDeclaredFields();
                    encryptObj(declaredFields, parameterObject);
                }
            }
        }
        //获取原方法的返回值
        return invocation.proceed();
    }

    /**
     * 一定要配置，加入此拦截器到拦截器链
     *
     * @param target
     * @return
     */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private static final String ENCRYPT_PREFIX = "_sensitive_start_";

    private <T> T encryptObj(Field[] aesFields, T paramsObject) {
        try {
            for (Field aesField : aesFields) {
                EncryptField filed = aesField.getAnnotation(EncryptField.class);
                if (!Objects.isNull(filed)) {
                    aesField.setAccessible(true);
                    Object object = aesField.get(paramsObject);
                    if (object instanceof String) {
                        String value = (String) object;
                        String encrypt = value;
                        if (!value.startsWith(ENCRYPT_PREFIX)) {
                            encrypt = DesKit.encrypt(KEY, value);
                            encrypt = ENCRYPT_PREFIX + encrypt;
                        }
                        aesField.set(paramsObject, encrypt);
                    }
                }
            }
            return paramsObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
