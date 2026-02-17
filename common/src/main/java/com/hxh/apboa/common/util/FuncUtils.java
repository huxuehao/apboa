package com.hxh.apboa.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 描述：公共工具类
 *
 * @author huxuehao
 **/
public class FuncUtils {
    private static final Map<String, String> EVN_VALUES_CACHE = new HashMap<>();

    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj instanceof Optional) {
            return ((Optional<?>)obj).isEmpty();
        } else if (obj instanceof CharSequence) {
            return ((CharSequence) obj).isEmpty();
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection<?>)obj).isEmpty();
        } else {
            return obj instanceof Map && ((Map<?, ?>) obj).isEmpty();
        }
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static String getEvn(String name) {
        String cacheValue = EVN_VALUES_CACHE.get(name);
        if (cacheValue != null) {
            return cacheValue;
        }

        String value = System.getenv(name);
        if (value != null) {
            EVN_VALUES_CACHE.put(name, value);
        }
        return value;
    }
    public static String getEvn(String name, String defaultVal) {
        String evn = getEvn(name);
        return evn == null ? defaultVal : evn;
    }

    public static String currentDataTime() {
        Date date = new Date();
        return getDataTime(date);
    }

    public static String getDataTime(Date date) {
        // 创建 SimpleDateFormat 并设置时区为东八区
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        // 格式化日期
        return sdf.format(date);
    }

    /**
     * 获取异常栈信息
     */
    public static String catchThrowableStackInfo(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
