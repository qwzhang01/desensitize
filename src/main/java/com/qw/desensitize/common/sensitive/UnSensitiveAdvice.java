package com.qw.desensitize.common.sensitive;

import com.qw.desensitize.common.R;
import com.qw.desensitize.dto.UnSensitiveDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.qw.desensitize.dto.UnSensitiveDto.UN_SENSITIVE_EMAIL;
import static com.qw.desensitize.dto.UnSensitiveDto.UN_SENSITIVE_PHONE;

/**
 * 接口返回字段脱敏拦截器
 * 使用说明
 * <p>
 * 使用方法，返回结果的类继承 com.qw.desensitize.dto.UnSensitiveDto
 * com.qw.desensitize.dto.UnSensitiveDto#sensitiveFlag，脱敏的标识，比如本人登录状态，则赋值为false，不脱敏，其他人登录查看则赋值为true脱敏
 * <p>
 * 需要脱敏的字段添加注解 com.qw.desensitize.common.sensitive.UnSensitive
 * com.qw.desensitize.common.sensitive.UnSensitive#type() 为脱敏算法，目前实现了手机，身份证，邮箱三种脱敏算法，对应枚举定位位置 com.qw.desensitize.dto.UnSensitiveDto
 *
 * @author avinzhang
 */
@ControllerAdvice
@AllArgsConstructor
@Slf4j
public class UnSensitiveAdvice implements ResponseBodyAdvice<R> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Type type = returnType.getGenericParameterType();
        String typeName = type.getTypeName();
        return typeName.startsWith("com.qw.desensitize.common.R");
    }

    @Nullable
    @Override
    public R beforeBodyWrite(@Nullable R body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null) {
            if (body.getData() != null) {
                if (body.getData() instanceof UnSensitiveDto) {
                    UnSensitiveDto sensitive = (UnSensitiveDto) body.getData();
                    if (sensitive.isSensitiveFlag()) {
                        Long start = System.currentTimeMillis();
                        body.setData(unSensitive(sensitive));
                        log.warn("脱敏耗时{}毫秒", System.currentTimeMillis() - start);
                        return body;
                    }
                } else if (body.getData() instanceof List) {
                    List<Object> list = (List<Object>) body.getData();
                    if (list != null && list.size() > 0) {
                        Object element = list.get(0);
                        if (element instanceof UnSensitiveDto) {
                            UnSensitiveDto sensitive = (UnSensitiveDto) element;
                            if (sensitive.isSensitiveFlag()) {
                                Long start = System.currentTimeMillis();
                                body.setData(unSensitive(list));
                                log.warn("脱敏耗时{}毫秒", System.currentTimeMillis() - start);
                                return body;
                            }
                        }
                    }
                }
            }
        }
        return body;
    }

    private Object unSensitive(Object data) {
        try {
            if (data instanceof List) {
                // 处理list
                List<Object> list = (List) data;
                for (Object o : list) {
                    unSensitive(o);
                }
            } else {
                // 处理类
                unSensitiveParam(data);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    /**
     * 脱敏
     *
     * @param data
     * @throws IllegalAccessException
     */
    private void unSensitiveParam(Object data) throws IllegalAccessException {
        if (data == null) {
            return;
        }
        List<Field> fields = getFields(data.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> classType = field.getType();
            if (classType.getName().startsWith("com.qw.desensitize.dto")) {
                // 如果属性是自定义类，递归处理
                unSensitiveParam(field.get(data));
            } else if (List.class.isAssignableFrom(classType)) {
                Object objList = field.get(data);
                if (objList != null) {
                    List<Object> dataList = (List<Object>) objList;
                    for (Object dataParam : dataList) {
                        unSensitiveParam(dataParam);
                    }
                }
            } else {
                UnSensitive annotation = field.getAnnotation(UnSensitive.class);
                if (annotation != null) {
                    String type = annotation.type();
                    if (UN_SENSITIVE_EMAIL.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, email(String.valueOf(field.get(data))));
                        }
                    }
                    if (UN_SENSITIVE_PHONE.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, phone(String.valueOf(field.get(data))));
                        }
                    }
                    if (UnSensitiveDto.UN_SENSITIVE_ID_NUM.equals(type)) {
                        if (field.get(data) != null) {
                            field.set(data, idNum(String.valueOf(field.get(data))));
                        }
                    }
                }
            }
        }
    }

    /**
     * 递归获取所有属性
     *
     * @param clazz
     * @return
     */
    private List<Field> getFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        list.addAll(Arrays.asList(declaredFields));


        Class<?> superclass = clazz.getSuperclass();
        if (superclass.getName().startsWith("com.qw.desensitize.dto")) {
            list.addAll(getFields(superclass));
        }
        return list;
    }

    /**
     * 脱敏邮箱
     *
     * @param src
     * @return
     */
    private String email(String src) {
        if (src == null) {
            return null;
        }
        String email = src.toString();
        int index = StringUtils.indexOf(email, "@");
        if (index <= 1) {
            return email;
        } else {
            return StringUtils.rightPad(StringUtils.left(email, 0), index, "*").concat(StringUtils.mid(email, index, StringUtils.length(email)));
        }
    }

    /**
     * 脱敏手机号码
     *
     * @param phone
     * @return
     */
    private String phone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return "";
        }
        return phone.replaceAll("(^\\d{0})\\d.*(\\d{4})", "$1****$2");
    }

    /**
     * 身份证脱敏
     *
     * @param idNumber
     * @return
     */
    private String idNum(String idNumber) {
        if (StringUtils.isBlank(idNumber)) {
            return "";
        }
        if (idNumber.length() == 15 || idNumber.length() == 18) {
            return idNumber.replaceAll("(\\w{4})\\w*(\\w{4})", "$1*********$2");
        }
        if (idNumber.length() > 4) {
            // 组织机构代码的方式脱敏****1111
            return idNumber.replaceAll("(\\w{0})\\w*(\\w{4})", "$1*********$2");
        }
        // 不足四位或者只有一位的都替代为*
        return "*********";
    }
}