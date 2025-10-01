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


package io.github.qwzhang01.desensitize.kit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring context utility class for accessing Spring-managed beans from non-Spring managed classes.
 *
 * <p>This utility provides static methods to retrieve beans from the Spring application context,
 * enabling access to Spring-managed components from classes that are not under Spring's control.
 * It implements ApplicationContextAware to automatically receive the application context during
 * Spring container initialization.</p>
 *
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Accessing Spring beans from utility classes</li>
 *   <li>Retrieving services from static methods</li>
 *   <li>Getting configuration beans from non-Spring components</li>
 * </ul>
 *
 * @author avinzhang
 * @see ApplicationContextAware
 * @since 1.0.0
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    /**
     * The Spring application context instance.
     * This is set automatically by Spring during container initialization.
     */
    private static ApplicationContext applicationContext;

    /**
     * Gets the Spring application context.
     *
     * @return the current application context, or null if not initialized
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Sets the application context. This method is called automatically by Spring
     * during the container initialization process.
     *
     * @param applicationContext the Spring application context
     * @throws BeansException if context setting fails
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * Retrieves a bean by its name from the Spring application context.
     *
     * @param name the name of the bean to retrieve
     * @return the bean instance
     * @throws BeansException if the bean cannot be found or created
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * Retrieves a bean by its type from the Spring application context.
     *
     * @param clazz the class type of the bean to retrieve
     * @param <T>   the generic type of the bean
     * @return the bean instance of the specified type
     * @throws BeansException if the bean cannot be found or created
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * Retrieves a bean by both name and type from the Spring application context.
     *
     * @param name  the name of the bean to retrieve
     * @param clazz the class type of the bean to retrieve
     * @param <T>   the generic type of the bean
     * @return the bean instance of the specified name and type
     * @throws BeansException if the bean cannot be found or created
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    /**
     * Checks whether the application context contains a bean with the specified name.
     *
     * @param name the name of the bean to check
     * @return true if the context contains a bean with the given name, false otherwise
     */
    public static boolean containsBean(String name) {
        return getApplicationContext().containsBean(name);
    }

    /**
     * Determines whether the bean with the given name is a singleton or prototype.
     * If no bean definition with the given name is found, a NoSuchBeanDefinitionException is thrown.
     *
     * @param name the name of the bean to check
     * @return true if the bean is a singleton, false if it's a prototype
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no bean definition is found
     */
    public static boolean isSingleton(String name) {
        return getApplicationContext().isSingleton(name);
    }

    /**
     * Gets the type of the bean with the specified name.
     *
     * @param name the name of the bean to check
     * @return the type of the bean, or null if not determinable
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no bean definition is found
     */
    public static Class<?> getType(String name) {
        return getApplicationContext().getType(name);
    }

    /**
     * Returns the aliases for the given bean name, if any are defined in the bean definition.
     *
     * @param name the bean name to check for aliases
     * @return an array of aliases, or an empty array if none
     * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if no bean definition is found
     */
    public static String[] getAliases(String name) {
        return getApplicationContext().getAliases(name);
    }

    /**
     * Checks whether the Spring application context has been initialized.
     *
     * @return true if the application context is available, false otherwise
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

    /**
     * Safely retrieves a bean by its type, returning null if the operation fails.
     * This method does not throw exceptions and is safe to use in scenarios where
     * bean availability is uncertain.
     *
     * @param clazz the class type of the bean to retrieve
     * @param <T>   the generic type of the bean
     * @return the bean instance of the specified type, or null if not available
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
     * Safely retrieves a bean by its name, returning null if the operation fails.
     * This method does not throw exceptions and is safe to use in scenarios where
     * bean availability is uncertain.
     *
     * @param name the name of the bean to retrieve
     * @return the bean instance, or null if not available
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
     * Safely retrieves a bean by both name and type, returning null if the operation fails.
     * This method does not throw exceptions and is safe to use in scenarios where
     * bean availability is uncertain.
     *
     * @param name  the name of the bean to retrieve
     * @param clazz the class type of the bean to retrieve
     * @param <T>   the generic type of the bean
     * @return the bean instance of the specified name and type, or null if not available
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