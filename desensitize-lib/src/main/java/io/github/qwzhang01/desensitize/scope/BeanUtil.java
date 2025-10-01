package io.github.qwzhang01.desensitize.scope;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.qwzhang01.desensitize.domain.CallCopy;
import io.github.qwzhang01.desensitize.exception.BeanCopyException;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean 操作工具
 *
 * @author avinzhang
 */
public class BeanUtil {
    public static boolean equals(Object object1, Object object2) {
        if (object1 == null && object2 == null) {
            return true;
        }
        if (object1 != null) {
            return object1.equals(object2);
        }
        return false;
    }

    public static <S, T> void setProperties(S source, T target) {
        BeanUtils.copyProperties(source, target);
    }

    public static <S, T> T copyProperties(S source, Class<T> targetType) {
        try {
            Constructor<T> target = targetType.getConstructor();
            T result = target.newInstance();
            BeanUtils.copyProperties(source, result);
            return result;
        } catch (Exception e) {
            throw new BeanCopyException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * 对象拷贝属性
     *
     * @param source
     * @param targetType
     * @param callback   符合条件的属性拷贝结束后，开始特殊属性拷贝处理
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T> T copyProperties(S source, Class<T> targetType, CallCopy<S, T> callback) {
        try {
            Constructor<T> target = targetType.getConstructor();
            T result = target.newInstance();
            BeanUtils.copyProperties(source, result);
            callback.call(source, result);
            return result;
        } catch (Exception e) {
            throw new BeanCopyException(e.getLocalizedMessage(), e);
        }
    }


    public static <S, T> List<T> copyToList(Collection<S> collection, Class<T> targetType) {
        if (null == collection) {
            return null;
        }
        if (collection.isEmpty()) {
            return new ArrayList<>(0);
        }

        return collection.stream().map((source) -> copyProperties(source, targetType)).collect(Collectors.toList());
    }

    /**
     * 列表拷贝属性
     *
     * @param collection
     * @param targetType
     * @param callback   符合条件的属性拷贝结束后，开始特殊属性拷贝处理
     * @param <T>
     * @param <S>
     * @return
     */
    public static <T, S> List<T> copyToList(Collection<S> collection, Class<T> targetType, CallCopy<S, T> callback) {
        if (null == collection) {
            return null;
        }
        if (collection.isEmpty()) {
            return new ArrayList<>(0);
        }

        return collection.stream().map((source) -> {
            T result = copyProperties(source, targetType);
            callback.call(source, result);
            return result;
        }).collect(Collectors.toList());
    }

    public static <S, T> Page<T> copyToPage(Page<S> page, Class<T> targetType) {
        Page<T> target = new Page<>();
        BeanUtils.copyProperties(page, target);
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            target.setRecords(copyToList(page.getRecords(), targetType));
        }
        return target;
    }

    /**
     * 分页列表拷贝属性
     *
     * @param page
     * @param targetType
     * @param callback
     * @param <S>
     * @param <T>
     * @return
     */
    public static <S, T> Page<T> copyToPage(Page<S> page, Class<T> targetType, CallCopy<S, T> callback) {
        Page<T> target = new Page<>();
        BeanUtils.copyProperties(page, target);
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            target.setRecords(copyToList(page.getRecords(), targetType, callback));
        }
        return target;
    }
}