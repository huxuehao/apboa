package com.hxh.apboa.core;

import com.hxh.apboa.common.enums.CodeLanguage;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.FuncUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 描述：
 *
 * @author huxuehao
 **/
public interface InstanceLoader<T> extends SmartInitializingSingleton {
    /**
     * 加载实例
     * @param codeSource 源代码
     * @return 实例
     */
    T loadInstance(String codeSource);

    /**
     * 获取语言
     * @return 语言
     */
    CodeLanguage getLanguage();

    /**
     * 获取对象实例
     * @param clazz 类
     * @return 对象实例
     */
    default Object getObject(Class<?> clazz) {
        Object instance;
        try {
            // 获取无参构造方法
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            // 若构造方法是私有的，需要设置可访问
            constructor.setAccessible(true);
            // 创建实例
            instance =  constructor.newInstance();

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("类 " + clazz.getName() + " 没有无参构造方法", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("类 " + clazz.getName() + " 无法实例化（可能是抽象类或接口）", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("类 " + clazz.getName() + " 的构造方法不可访问", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("类 " + clazz.getName() + " 的构造方法执行失败", e.getTargetException());
        }
        return instance;
    }

    /**
     * 获取代码的identity
     * @param codeSource 代码源
     */
    default String codeIdentity(String codeSource){
        byte[] md5;
        try {
            md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return codeSource;
        }
        return new BigInteger(1, md5).toString(16);
    }

    /**
     * 依赖注入，从spring容器中获取bean，进行依赖注入
     *
     * @param instance Glue对象实例
     */
    default void dependencyInjection(Object instance) {
        if (instance == null) {
            return;
        }

        if (!BeanUtils.checkBeanFactory()) {
            return;
        }

        Field[] declaredFields = instance.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            // 排除静态字段
            if (Modifier.isStatic(declaredField.getModifiers())) {
                continue;
            }

            /*
             * 判断当前filed是否需要注入依赖，并重spring中获取依赖
             */

            Object beanObj = null;
            Resource resource;
            // 成立条件：判断当前Field是否被 @Resource 标记
            if ((resource = AnnotationUtils.getAnnotation(declaredField, Resource.class)) != null) {
                try {
                    if (!FuncUtils.isEmpty(resource.name())) {
                        beanObj = BeanUtils.getBean(resource.name());
                    } else {
                        beanObj = BeanUtils.getBean(declaredField.getName());
                    }
                } catch (Exception ignored) {
                }
                if (beanObj == null) {
                    beanObj = BeanUtils.getBean(declaredField.getType());
                }
            }
            // 成立条件：判断当前Field是否被 @Autowired 标记
            else if (AnnotationUtils.getAnnotation(declaredField, Autowired.class) != null) {
                Qualifier qualifier = AnnotationUtils.getAnnotation(declaredField, Qualifier.class);
                if (qualifier != null && !FuncUtils.isEmpty(qualifier.value())) {
                    beanObj = BeanUtils.getBean(qualifier.value());
                } else {
                    beanObj = BeanUtils.getBean(declaredField.getType());
                }
            }

            if (beanObj != null) {
                declaredField.setAccessible(true);
                try {
                    declaredField.set(instance, beanObj);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }

}
