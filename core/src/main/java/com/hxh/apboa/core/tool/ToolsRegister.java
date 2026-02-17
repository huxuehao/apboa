package com.hxh.apboa.core.tool;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：工具类工厂
 *
 * @author huxuehao
 **/
public class ToolsRegister {
    private static final ConcurrentHashMap<String, IAgentTool> tools = new ConcurrentHashMap<>();

    public static void register(String classPath, IAgentTool tool) {
        tools.put(classPath, tool);
    }

    public static IAgentTool getTool(String classPath) {
        return tools.get(classPath);
    }

    public static List<IAgentTool> getTools() {
        return tools.values().stream().toList();
    }

    public static void unregisterTool(String classPath) {
        tools.remove(classPath);
    }

    public static void clear(Class<? extends IAgentTool> toolClass) {
        tools.clear();
    }
}
