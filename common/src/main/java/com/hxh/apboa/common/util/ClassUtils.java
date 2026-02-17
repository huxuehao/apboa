package com.hxh.apboa.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * 描述：Class工具类
 *
 * @author huxuehao
 **/
public class ClassUtils {
    /**
     * 获取与参数Class同包下的实现类
     */
    public static List<Class<?>> getClassByImplements(Class<?> c) {
        if (c.isInterface()) {
            return getClassOfAssignable(c);
        } else {
            throw new IllegalArgumentException("参数不合法，getAllClassByInterface(Class c)的参数必须为接口");
        }
    }

    /**
     * 获取在packageNames下的参数Class的实现类
     * @param c 接口Class
     * @param packageNames 指定包集合
     */
    public static List<Class<?>> getClassByImplements(Class<?> c, List<String> packageNames) {
        if (c.isInterface()) {
            return getClassOfAssignable(c, packageNames);
        } else {
            throw new IllegalArgumentException("参数不合法，getAllClassByInterface(Class c)的参数必须为接口");
        }
    }

    /**
     * 获取与参数Class同包下的子类
     */
    public static List<Class<?>> getClassByExtends(Class<?> c) {
        if (c.isLocalClass()) {
            return getClassOfAssignable(c);
        } else {
            throw new IllegalArgumentException("参数不合法，getAllClassByInterface(Class c)的参数必须为类");
        }
    }

    /**
     * 获取在packageNames下的参数Class的子类
     */
    public static List<Class<?>> getClassByExtends(Class<?> c, List<String> packageNames) {
        if (c.isLocalClass()) {
            return getClassOfAssignable(c, packageNames);
        } else {
            throw new IllegalArgumentException("参数不合法，getAllClassByInterface(Class c)的参数必须为类");
        }
    }

    /**
     * 获取与参数Class同包下的子类和实现类
     */
    public static List<Class<?>> getClassOfAssignable(Class<?> c) {
        ArrayList<Class<?>> classes = new ArrayList<>();
        String packageName = c.getPackage().getName();
        List<Class<?>> allClass;
        try {
            allClass = getClasses(packageName);
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
        for (Class<?> aClass : allClass) {
            if (c.isAssignableFrom(aClass) && !c.equals(aClass)) {
                classes.add(aClass);
            }
        }
        return classes;
    }

    /**
     * 获取在packageNames下的参数Class的实现类和子类
     */
    public static List<Class<?>> getClassOfAssignable(Class<?> c, List<String> packageNames) {
        List<Class<?>> classes = new ArrayList<>();
        List<Class<?>> allClass = new ArrayList<>();
        if (packageNames == null || packageNames.size() == 0) {
            return classes;
        }
        try {
            for (String packageName : packageNames) {
                allClass.addAll(getClasses(packageName));
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
        for (Class<?> aClass : allClass) {
            if (c.isAssignableFrom(aClass) && !c.equals(aClass)) {
                classes.add(aClass);
            }
        }
        return classes;
    }

    /**
     * 从一个包中查找出所有的类
     * @param packageName 包名
     */
    public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        /* 获取类加载器 */
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        /* 包名转路径 */
        String path = packageName.replace('.', '/');
        /* 获取资源 */
        Enumeration<URL> resources = classLoader.getResources(path);
        /* 获取目录 */
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            dirs.add(new File(resources.nextElement().getFile()));
        }
        /* 从目录中获取类 */
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * 寻找类
     * @param directory file
     * @param packageName 包名
     */
    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            /* 判断是否为文件夹 */
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                /* 若是文件夹，则进行递归调用 */
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                /* 根据包名获取类 */
                classes.add(Class.forName(packageName + "." + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}
