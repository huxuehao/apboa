package com.hxh.apboa.core.endpoint;

import com.hxh.apboa.core.tool.IAgentTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * 描述：工具反射执行器
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class ToolReflectionInvoker {
    /**
     * 反射调用Tool方法
     * @param toolInstance Tool实例
     * @param toolName Tool名称
     * @param params 参数Map
     * @return 调用结果
     */
    public Object invokeTool(IAgentTool toolInstance, String toolName, Map<String, Object> params) {
        try {
            // 获取目标方法
            Method targetMethod = findToolMethod(toolInstance.getClass(), toolName);

            if (targetMethod == null) {
                throw new IllegalArgumentException("未找到Tool方法: " + toolName);
            }

            // 构建方法参数
            Object[] args = buildMethodArgs(targetMethod, params);

            // 设置方法可访问并调用
            targetMethod.setAccessible(true);
            Object result = targetMethod.invoke(toolInstance, args);

            log.info("Tool方法调用成功: {}, 参数: {}", toolName, params);
            return result;

        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Tool方法调用失败: {}", toolName, e);
            throw new RuntimeException("Tool方法调用失败", e);
        }
    }

    /**
     * 查找对应的Tool方法
     */
    private Method findToolMethod(Class<?> clazz, String toolName) {
        for (Method method : clazz.getDeclaredMethods()) {
            Tool tool = method.getAnnotation(Tool.class);
            if (tool != null && tool.name().equals(toolName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 构建方法参数
     */
    private Object[] buildMethodArgs(Method method, Map<String, Object> params) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ToolParam toolParam = parameter.getAnnotation(ToolParam.class);

            if (toolParam != null) {
                String paramName = toolParam.name();
                Object value = params.get(paramName);

                // 参数验证
                if (value == null && toolParam.required()) {
                    throw new IllegalArgumentException(
                            String.format("参数[%s]是必需的", paramName)
                    );
                }

                // 类型转换（如果需要）
                args[i] = convertIfNeeded(value, parameter.getType());
            } else {
                // 没有注解的参数，尝试按位置匹配
                args[i] = params.get(String.valueOf(i));
            }
        }

        return args;
    }

    /**
     * 简单的类型转换
     */
    private Object convertIfNeeded(Object value, Class<?> targetType) {
        if (value == null) return null;

        // 如果类型匹配，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        // 简单的类型转换示例
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        // 可以根据需要添加更多类型转换

        return value;
    }
}
