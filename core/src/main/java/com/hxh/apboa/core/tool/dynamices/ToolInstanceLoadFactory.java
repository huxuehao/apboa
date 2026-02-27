package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.common.enums.CodeLanguage;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：实例加载工厂
 *
 * @author huxuehao
 **/
public class ToolInstanceLoadFactory {
    private static final Map<CodeLanguage, ToolInstanceLoader> loaders = new HashMap<>();

    /**
     * 注册加载器
     */
    public static void registerLoader(ToolInstanceLoader loader) {
        loaders.put(loader.getLanguage(), loader);
    }

    /**
     * 获取加载器
     */
    public static ToolInstanceLoader getInstanceLoader(CodeLanguage language) {
        ToolInstanceLoader toolInstanceLoader = loaders.get(language);
        if (toolInstanceLoader == null) {
            throw new RuntimeException("No loader found for language " + language);
        } else {
            return toolInstanceLoader;
        }
    }
}
