package io.github.qwzhang01.desensitize.core;

import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.EncryptionAlgoContainer;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * ResultSetHandler 拦截
 * ResultSetHandler 负责将数据库查询结果集映射为 Java 对象。拦截 ResultSetHandler 可以自定义结果集的处理逻辑。
 * <p>
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
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class)})
public class EncryptInterceptor implements Interceptor {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EncryptInterceptor.class);
    private static final String ENCRYPT_PREFIX = "_sensitive_start_";
    private final static Set<Class<?>> NO_CLASS = new CopyOnWriteArraySet<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // @Signature 指定了 type= parameterHandler 后，这里的 invocation.getTarget() 便是parameterHandler
        // 若指定ResultSetHandler ，这里则能强转为ResultSetHandler
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();

        MetaObject metaObject = MetaObject.forObject(parameterHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
        //sql语句类型：UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH、update,SqlCommandType是个enum
        String sqlCommandType = mappedStatement.getSqlCommandType().toString();
//        if (!"INSERT".equals(sqlCommandType) && !"UPDATE".equals(sqlCommandType)) {
//            return invocation.proceed();
//        }

        // 获取参数对象，即mapper中paramsType的实例
        Field paramsFiled = parameterHandler.getClass().getDeclaredField("parameterObject");
        // 将此对象的 accessible 标志设置为指示的布尔值。值为 true 则指示反射的对象在使用时应该取消 Java 语言访问检查。
        paramsFiled.setAccessible(true);
        // 取出实例
        Object parameterObject = paramsFiled.get(parameterHandler);
        List<Backup> plaintextBackup = new ArrayList<>();
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
            if (parameterObjectClass != null && needToDecrypt(parameterObject)) {
                //取出当前类的所有字段，传入加密方法
                Field[] declaredFields = parameterObjectClass.getDeclaredFields();
                encryptObj(declaredFields, parameterObject, plaintextBackup);
            }
        }
        //获取原方法的返回值
        Object proceed = invocation.proceed();

        restorePlaintext(plaintextBackup);
        return proceed;
    }

    private boolean needToDecrypt(Object object) {
        Class<?> objectClass = object.getClass();
        return !NO_CLASS.contains(objectClass);
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

    private <T> boolean encryptObj(Field[] aesFields, T paramsObject, List<Backup> plaintextBackup) {
        boolean e = false;
        try {
            for (Field aesField : aesFields) {
                EncryptField annotation = aesField.getAnnotation(EncryptField.class);
                if (!Objects.isNull(annotation)) {
                    aesField.setAccessible(true);
                    e = true;
                    Object object = aesField.get(paramsObject);
                    if (object instanceof String value) {
                        // 备份明文
                        plaintextBackup.add(new Backup(aesField, value, paramsObject));
                        // 加密
                        String encrypt = value;
                        if (!value.startsWith(ENCRYPT_PREFIX)) {
                            encrypt = EncryptionAlgoContainer.getAlgo(annotation.value()).encrypt(value);
                            encrypt = ENCRYPT_PREFIX + encrypt;
                        }
                        aesField.set(paramsObject, encrypt);
                    }
                } else {
                    Class<?> type = aesField.getType();
                    if (ClazzUtil.isWrapper(type)) {
                        aesField.setAccessible(true);
                        Object object = aesField.get(paramsObject);
                        if (object != null && needToDecrypt(object)) {
                            e = encryptObj(object.getClass().getDeclaredFields(), object, plaintextBackup);
                        }
                    }
                }
            }
            if (!e) {
                NO_CLASS.add(paramsObject.getClass());
            }
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    // 还原明文
    private void restorePlaintext(List<Backup> plaintextBackup) {
        for (Backup entry : plaintextBackup) {
            Field field = entry.field;
            String plaintext = entry.value;
            try {
                field.setAccessible(true);
                field.set(entry.object, plaintext);
                log.debug("字段 {} 已还原为明文", field.getName());
            } catch (Exception e) {
                log.error("还原字段 {} 失败", field.getName(), e);
            }
        }
    }

    private record Backup(Field field, String value, Object object) {
    }
}
