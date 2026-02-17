package com.hxh.apboa.core.tool;

import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.wrapper.ToolInfoWrapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 描述：代理工具接口
 *
 * @author huxuehao
 **/
public interface IAgentTool extends SmartInitializingSingleton {

    default ToolInfoWrapper parseToolInfo() {
        try {
            // 遍历类中的所有方法
            for (Method method : this.getClass().getDeclaredMethods()) {
                // 检查方法是否有 @Tool 注解
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolInfoWrapper toolInfo = new ToolInfoWrapper();
                    toolInfo.setClassPath(this.getClass().getName());

                    Tool toolAnnotation = method.getAnnotation(Tool.class);
                    if (toolAnnotation != null) {
                        toolInfo.setName(toolAnnotation.name());
                        toolInfo.setDescription(toolAnnotation.description());
                    }

                    // 解析方法参数
                    List<ToolInfoWrapper.ParamInfo> params = getParamInfos(method);

                    toolInfo.setParams(params);
                    return toolInfo; // 返回第一个找到的 @Tool 方法
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static List<ToolInfoWrapper.ParamInfo> getParamInfos(Method method) {
        List<ToolInfoWrapper.ParamInfo> params = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        for (Parameter param : parameters) {
            // 检查参数是否有 @ToolParam 注解
            if (param.isAnnotationPresent(ToolParam.class)) {
                ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
                ToolInfoWrapper.ParamInfo paramInfo = new ToolInfoWrapper.ParamInfo();
                if (paramAnnotation != null) {
                    paramInfo.setName(paramAnnotation.name());
                }
                if (paramAnnotation != null) {
                    paramInfo.setDescription(paramAnnotation.description());
                }
                if (paramAnnotation != null) {
                    paramInfo.setRequired(paramAnnotation.required());
                } else {
                    paramInfo.setRequired(false);
                }
                paramInfo.setType(extractParameterType(param));
                params.add(paramInfo);
            }
        }
        return params;
    }

    private static String extractParameterType(Parameter parameter) {
        Class<?> type = parameter.getType();
        Type genericType = parameter.getParameterizedType();

        // 处理基本类型和包装类
        if (type == int.class || type == Integer.class) {
            return "integer";
        } else if (type == long.class || type == Long.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class) {
            return "number";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }

        // 处理数组类型
        if (type.isArray()) {
            return "array";
        }

        // 处理集合类型（List、Set等）
        if (Collection.class.isAssignableFrom(type)) {
            return "array";
        }

        // 处理Map类型
        if (Map.class.isAssignableFrom(type)) {
            return "object";
        }

        // 处理泛型数组（如List<String>[]）
        if (genericType instanceof GenericArrayType) {
            return "array";
        }

        // 处理参数化类型（泛型）
        if (genericType instanceof ParameterizedType pType) {
            Class<?> rawType = (Class<?>) pType.getRawType();

            if (Collection.class.isAssignableFrom(rawType)) {
                return "array";
            } else if (Map.class.isAssignableFrom(rawType)) {
                return "object";
            }
        }

        // 其他所有类型都返回 string
        return "string";
    }

    /**
     *  将对象转换为json字符串
     */
    default String toJsonString(Object obj) {
        return JsonUtils.toJsonStr(obj);
    }

    default void afterSingletonsInstantiated() {
        ToolsRegister.register(this.getClass().getName(), this);
    }
}
