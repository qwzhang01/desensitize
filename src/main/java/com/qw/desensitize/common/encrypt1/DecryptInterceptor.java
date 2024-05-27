package com.qw.desensitize.common.encrypt1;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.qw.desensitize.kit.DesKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;

/**
 * 这里是对找出来的字符串结果集进行解密所以是ResultSetHandler
 * args是指定预编译语句
 *
 * @author avinzhang
 */
@Slf4j
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class DecryptInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //取出查询的结果
        Object resultObject = invocation.proceed();
        if (Objects.isNull(resultObject)) {
            return null;
        }
        //基于selectList
        if (resultObject instanceof ArrayList) {
            ArrayList resultList = (ArrayList) resultObject;
            if (!CollectionUtils.isEmpty(resultList) && needToDecrypt(resultList.get(0))) {
                for (Object result : resultList) {
                    // 逐一解密
                    decryptObj(result);
                }
            }
            // 基于selectOne
        } else {
            if (needToDecrypt(resultObject)) {
                decryptObj(resultObject);
            }
        }
        return resultObject;
    }

    /**
     * 对单个结果集判空的一个方法
     *
     * @param object
     * @return
     */
    private boolean needToDecrypt(Object object) {
        Class<?> objectClass = object.getClass();
        EncryptObj sensitiveData = AnnotationUtils.findAnnotation(objectClass, EncryptObj.class);
        return Objects.nonNull(sensitiveData);
    }

    /**
     * 将此过滤器加入到过滤器链当中
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

    private <T> T decryptObj(T result) {
        try {
            Class<?> resultClass = result.getClass();
            Field[] declaredFields = resultClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                //去除所有被EncryptDecryptFiled注解的字段
                EncryptField sensitiveFiled = declaredField.getAnnotation(EncryptField.class);
                if (!Objects.isNull(sensitiveFiled)) {
                    //将此对象的 accessible 标志设置为指示的布尔值。值为 true 则指示反射的对象在使用时应该取消 Java 语言访问检查。
                    declaredField.setAccessible(true);
                    //这里的result就相当于是字段的访问器
                    Object object = declaredField.get(result);
                    //只支持String解密
                    if (object instanceof String) {
                        String value = (String) object;
                        //修改：没有标识则不解密
                        if (value.startsWith(ENCRYPT_PREFIX)) {
                            value = value.substring(17);
                            value = DesKit.decrypt(DesKit.KEY, value);
                        }
                        //对注解在这段进行逐一解密
                        declaredField.set(result, value);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

