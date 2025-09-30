package io.github.qwzhang01.desensitize.kit;

import io.github.qwzhang01.desensitize.annotation.Mask;
import io.github.qwzhang01.desensitize.shield.CoverAlgo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

/**
 * 脱敏工具类
 * 提供便捷的脱敏方法，自动从 Spring 容器获取脱敏算法实现
 *
 * @author qwzhang01
 */
public class MaskAlgoContainer {
    public Object mask(Object data) {
        try {
            if (data instanceof List<?> list) {
                // 处理list
                for (Object o : list) {
                    mask(o);
                }
            } else {
                // 处理类
                maskParam(data);
            }
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
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
    private void maskParam(Object data) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (data == null) {
            return;
        }
        List<Field> fields = getFields(data.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            if (List.class.isAssignableFrom(field.getType())) {
                Object objList = field.get(data);
                if (objList != null) {
                    List<Object> dataList = (List<Object>) objList;
                    for (Object dataParam : dataList) {
                        maskParam(dataParam);
                    }
                }
            } else if (!ClazzUtil.isWrapper(field.getType())) {
                // 如果属性是自定义类，递归处理
                maskParam(field.get(data));
            } else {
                Mask annotation = field.getAnnotation(Mask.class);
                if (annotation != null) {
                    Class<? extends CoverAlgo> clazz = annotation.value();
                    CoverAlgo coverAlgo = clazz.getDeclaredConstructor().newInstance();
                    if (field.get(data) != null) {
                        field.set(data, coverAlgo.mask(String.valueOf(field.get(data))));
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
        Field[] declaredFields = clazz.getDeclaredFields();
        return Arrays.asList(declaredFields);
    }
}