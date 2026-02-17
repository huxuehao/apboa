package com.hxh.apboa.common.mp.support;

import com.hxh.apboa.common.mp.annotation.QueryDefine;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 描述：条件构造器
 *
 * @author huxuehao
 */
@Slf4j
public class ConditionBuilder {
    public ConditionBuilder() {
    }

    /**
     * 构造查询条件
     * @param t  实体
     * @param qw QueryWrapper
     */
    public static <T> void buildCondition(T t, QueryWrapper<?> qw) {
        getAllDeclaredFields(t.getClass()).stream().peek((field) -> {
            field.setAccessible(true);
        }).filter((field) -> {
            try {
                return ObjectUtils.isNotEmpty(field.get(t)) && field.getAnnotation(QueryDefine.class) != null;
            } catch (IllegalAccessException var3) {
                return false;
            }
        }).forEach((field) -> {
            try {
                QueryDefine tableField = field.getAnnotation(QueryDefine.class);
                QueryCondition sqlOperator = null;
                if (tableField != null) {
                    sqlOperator = tableField.condition();
                }
                Object value = field.get(t);
                buildWhere(humpToUnderline(field.getName()), value, sqlOperator, qw);
            } catch (Exception var6) {
                log.error(var6.getMessage(), var6);
            }

        });
    }

    /**
     * 获取类中的所有字段（包括父类中的）
     * @param clazz 目标类
     */
    public static List<Field> getAllDeclaredFields(Class<?> clazz) {
        Class<?> currentClass = clazz;
        HashMap<String, Field> fieldMap = new HashMap<>();
        while (currentClass != null) {
            // 添加当前类中声明的所有字段，存在则保持不变，不存在则添加
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                fieldMap.compute(declaredField.getName(), (k, v) -> (v == null ? declaredField : v));
            }
            // 移动到父类
            currentClass = currentClass.getSuperclass();
        }
        return new ArrayList<>(fieldMap.values());
    }

    /**
     * 构造查询条件
     */
    private static void buildWhere(String name, Object value, QueryCondition rule, QueryWrapper<?> qw) {
        if (value != null && rule != null) {
            switch (rule) {
                case GT:
                    qw.gt(name, value);
                    break;
                case GE:
                    qw.ge(name, value);
                    break;
                case LT:
                    qw.lt(name, value);
                    break;
                case LE:
                    qw.le(name, value);
                    break;
                case NE:
                    qw.ne(name, value);
                    break;
                case IN:
                    if (value instanceof String) {
                        qw.in(name, (Object[])value.toString().split(","));
                    } else if (value instanceof String[]) {
                        qw.in(name, (Object[]) value);
                    } else {
                        qw.in(name, value);
                    }
                    break;
                case BETWEEN:
                    Object[] temp;
                    if (value instanceof String) {
                        temp = value.toString().split(",");
                        qw.between(name, temp[0], temp[1]);
                    } else if (value instanceof String[]) {
                        temp = (Object[]) value;
                        qw.between(name, temp[0], temp[1]);
                    }
                    break;
                case LIKE:
                    qw.like(name, value);
                    break;
                case LEFT_LIKE:
                    qw.likeLeft(name, value);
                    break;
                case RIGHT_LIKE:
                    qw.likeRight(name, value);
                    break;
                case EQ:
                default:
                    qw.eq(name, value);
            }
        }
    }

    /**
     * 驼峰转下划线
     */
    private static String humpToUnderline(String para) {
        para = firstCharToLower(para);
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;

        for(int i = 0; i < para.length(); ++i) {
            if (Character.isUpperCase(para.charAt(i))) {
                sb.insert(i + temp, "_");
                ++temp;
            }
        }

        return sb.toString().toLowerCase();
    }

    private static String firstCharToLower(String str) {
        char firstChar = str.charAt(0);
        if (firstChar >= 'A' && firstChar <= 'Z') {
            char[] arr = str.toCharArray();
            arr[0] = (char)(arr[0] + 32);
            return new String(arr);
        } else {
            return str;
        }
    }
}
