package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.common.enums.CodeLanguage;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：实例加载工厂
 *
 * @author huxuehao
 **/
public class InstanceLoadFactory {
    private static final Map<CodeLanguage, InstanceLoader> loaders = new HashMap<>();

    /**
     * 注册加载器
     */
    public static void registerLoader(InstanceLoader loader) {
        loaders.put(loader.getLanguage(), loader);
    }

    /**
     * 获取加载器
     */
    public static InstanceLoader getInstanceLoader(CodeLanguage language) {
        InstanceLoader instanceLoader = loaders.get(language);
        if (instanceLoader == null) {
            throw new RuntimeException("No loader found for language " + language);
        } else {
            return instanceLoader;
        }
    }
}
