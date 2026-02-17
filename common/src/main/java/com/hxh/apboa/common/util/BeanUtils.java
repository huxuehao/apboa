package com.hxh.apboa.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述：spring工具类,方便在非spring管理环境中获取bean
 *
 * @author huxuehao
 **/
@Component
public final class BeanUtils implements BeanFactoryPostProcessor {
    /** Spring应用上下文环境 */
    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        BeanUtils.beanFactory = beanFactory;
    }

    public static boolean checkBeanFactory() {
        return beanFactory != null;
    }
    /**
     * 获取对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) throws BeansException {
        return (T) beanFactory.getBean(name);
    }

    /**
     * 获取类型为requiredType的对象
     *
     */
    public static <T> T getBean(Class<T> clz) throws BeansException {
        return (T) beanFactory.getBean(clz);
    }

    /**
     * 如果BeanFactory包含一个与所给名称匹配的bean定义，则返回true
     */
    public static boolean containsBean(String name)
    {
        return beanFactory.containsBean(name);
    }

    /**
     * 判断以给定名字注册的bean定义是一个singleton还是一个prototype。
     * 如果与给定名字相应的bean定义没有被找到，将会抛出一个异常（NoSuchBeanDefinitionException）
     *
     */
    public static boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.isSingleton(name);
    }

    /**
     * 根据Bean的名称获取Bean的类型
     *
     */
    public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.getType(name);
    }

    /**
     * 如果给定的bean名字在bean定义中有别名，则返回这些别名
     *
     */
    public static String[] getAliases(String name) throws NoSuchBeanDefinitionException {
        return beanFactory.getAliases(name);
    }


    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType){
        return beanFactory.getBeansWithAnnotation(annotationType);
    }

    /**
     * 获取aop代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAopProxy(T invoker) {
        return (T) AopContext.currentProxy();
    }

    /**
     * 属性拷贝
     */
    public static <T> T copy(Object source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        T target = org.springframework.beans.BeanUtils.instantiateClass(clazz);
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 集合拷贝
     */
    public static <T, S> List<T> copyList(List<S> sourceList, Class<T> clazz) {
        if (sourceList == null) {
            return new ArrayList<>();
        }
        return sourceList.stream().map(s -> copy(s, clazz)).collect(Collectors.toList());
    }

    /**
     * 分页拷贝
     */
    public static <T, S> IPage<T> copyPage(IPage<S> sourcePage, Class<T> clazz) {
        if (sourcePage == null) {
            return null;
        }
        IPage<T> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(copyList(sourcePage.getRecords(), clazz));
        return targetPage;
    }
}
