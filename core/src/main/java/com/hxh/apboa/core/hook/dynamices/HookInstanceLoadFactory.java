package com.hxh.apboa.core.hook.dynamices;

import com.hxh.apboa.common.enums.CodeLanguage;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：实例加载工厂
 *
 * @author huxuehao
 **/
public class HookInstanceLoadFactory {
    private static final Map<CodeLanguage, HookInstanceLoader> loaders = new HashMap<>();

    /**
     * 注册加载器
     */
    public static void registerLoader(HookInstanceLoader loader) {
        loaders.put(loader.getLanguage(), loader);
    }

    /**
     * 获取加载器
     */
    public static HookInstanceLoader getInstanceLoader(CodeLanguage language) {
        HookInstanceLoader hookInstanceLoader = loaders.get(language);
        if (hookInstanceLoader == null) {
            throw new RuntimeException("No loader found for language " + language);
        } else {
            return hookInstanceLoader;
        }
    }
}
