package com.hxh.apboa.core.hook;

import io.agentscope.core.hook.Hook;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：钩子注册类，用于注册各种钩子
 *
 * @author huxuehao
 **/
public class HooksRegister {
    private static final ConcurrentHashMap<String, Hook> hooks = new ConcurrentHashMap<>();

    public static void register(String classPath, IAgentHook hook) {
        hooks.put(classPath, hook);
    }

    public static Hook getHook(String classPath) {
        return hooks.get(classPath);
    }

    public static List<Hook> getHooks() {
        return hooks.values().stream().toList();
    }

    public static void unregister(String classPath) {
        hooks.remove(classPath);
    }

    public static void clear() {
        hooks.clear();
    }

}
