/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.github.qwzhang01.desensitize.core;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import io.github.qwzhang01.desensitize.annotation.EncryptField;
import io.github.qwzhang01.desensitize.exception.DesensitizeException;
import io.github.qwzhang01.desensitize.kit.ClazzUtil;
import io.github.qwzhang01.desensitize.kit.EncryptionAlgoContainer;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ParameterHandler 拦截
 * ParameterHandler 负责将 SQL 参数绑定到预编译语句（PreparedStatement）中。拦截 ParameterHandler 可以控制参数的设置过程。
 * <p>
 * 这里是对找出来的字符串结果集进行解密所以是ResultSetHandler
 * args是指定预编译语句
 *
 * @author avinzhang
 */
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class DecryptInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 取出查询的结果
        Object resultObject = invocation.proceed();
        if (Objects.isNull(resultObject)) {
            return null;
        }
        // 基于selectList
        if (resultObject instanceof ArrayList) {
            List<?> resultList = (ArrayList<?>) resultObject;
            if (!CollectionUtils.isEmpty(resultList)) {
                for (Object result : resultList) {
                    List<ClazzUtil.AnnotatedFieldResult<EncryptField>> fields =
                            ClazzUtil.getAnnotatedFields(result, EncryptField.class);
                    // 逐一解密
                    decryptObj(fields);
                }
            }
            // 基于selectOne
        } else {
            List<ClazzUtil.AnnotatedFieldResult<EncryptField>> fields =
                    ClazzUtil.getAnnotatedFields(resultObject, EncryptField.class);
            decryptObj(fields);
        }
        return resultObject;
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

    private void decryptObj(List<ClazzUtil.AnnotatedFieldResult<EncryptField>> fields) {

        try {
            for (ClazzUtil.AnnotatedFieldResult<EncryptField> fieldObj : fields) {
                EncryptField annotation = fieldObj.annotation();
                Field field = fieldObj.field();
                Object object = fieldObj.containingObject();
                Object value = fieldObj.getFieldValue();

                field.setAccessible(true);
                if (value instanceof String strValue) {
                    strValue = EncryptionAlgoContainer.getAlgo(annotation.value()).decrypt(strValue);
                    //对注解在这段进行逐一解密
                    field.set(object, strValue);
                }
            }
        } catch (Exception e) {
            throw new DesensitizeException(e);
        }
    }
}

