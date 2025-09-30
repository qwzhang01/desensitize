package io.github.qwzhang01.desensitize.kit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring 上下文工具类
 * 用于在非 Spring 管理的类中获取 Spring 容器中的 Bean
 *
 * @author qwzhang01
 */
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 通过 name 获取 Bean
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过 class 获取 Bean
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过 name 和 class 获取指定的 Bean
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    /**
     * 检查是否包含指定名称的 Bean
     *
     * @param name Bean 名称
     * @return 是否包含
     */
    public static boolean containsBean(String name) {
        return getApplicationContext().containsBean(name);
    }

    /**
     * 判断以给定名字注册的 Bean 定义是一个 singleton 还是一个 prototype
     * 如果与给定名字相应的 Bean 定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
     *
     * @param name Bean 名称
     * @return 是否为单例
     */
    public static boolean isSingleton(String name) {
        return getApplicationContext().isSingleton(name);
    }

    /**
     * 获取指定 Bean 的类型
     *
     * @param name Bean 名称
     * @return Bean 类型
     */
    public static Class<?> getType(String name) {
        return getApplicationContext().getType(name);
    }

    /**
     * 如果给定的 Bean 名字在 Bean 定义中有别名，则返回这些别名
     *
     * @param name Bean 名称
     * @return Bean 别名数组
     */
    public static String[] getAliases(String name) {
        return getApplicationContext().getAliases(name);
    }

    /**
     * 检查 ApplicationContext 是否已初始化
     *
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

    /**
     * 安全获取 Bean，如果获取失败返回 null
     *
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例或 null
     */
    public static <T> T getBeanSafely(Class<T> clazz) {
        try {
            if (!isInitialized()) {
                return null;
            }
            return getBean(clazz);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 安全获取 Bean，如果获取失败返回 null
     *
     * @param name Bean 名称
     * @return Bean 实例或 null
     */
    public static Object getBeanSafely(String name) {
        try {
            if (!isInitialized()) {
                return null;
            }
            return getBean(name);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 安全获取 Bean，如果获取失败返回 null
     *
     * @param name  Bean 名称
     * @param clazz Bean 类型
     * @param <T>   泛型
     * @return Bean 实例或 null
     */
    public static <T> T getBeanSafely(String name, Class<T> clazz) {
        try {
            if (!isInitialized()) {
                return null;
            }
            return getBean(name, clazz);
        } catch (BeansException e) {
            return null;
        }
    }
}