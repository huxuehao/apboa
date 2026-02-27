package com.hxh.apboa.core.tool.dynamices.loader;

import com.hxh.apboa.common.enums.CodeLanguage;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;
import com.hxh.apboa.core.tool.dynamices.ToolInstanceLoader;
import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 描述：Groovy实例加载器
 *
 * @author huxuehao
 **/
@Component
public class GroovyToolInstanceLoader implements ToolInstanceLoader {
    private final GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
    private final ConcurrentMap<String, IDynamicAgentTool> TOOL_OBJ_CACHE = new ConcurrentHashMap<>();

    /**
     * 根据源码加载实例
     *
     * @param codeSource 源码
     */
    @Override
    public IDynamicAgentTool loadInstance(String codeSource) {
        if (!FuncUtils.isEmpty(codeSource)) {
            if (TOOL_OBJ_CACHE.containsKey(codeIdentity(codeSource))) {
                return TOOL_OBJ_CACHE.get(codeIdentity(codeSource));
            }

            // 基于源码获取Class
            Class<?> clazz = groovyClassLoader.parseClass(codeSource);;
            if (clazz == null) {
                throw new IllegalArgumentException("loadNewInstance 执行失败, Glue 脚本为空");
            }

            Object instance = getObject(clazz);
            if (!(instance instanceof IDynamicAgentTool)) {
                throw new IllegalArgumentException("loadNewInstance 执行失败, Glue 脚本类需要继承 " + IDynamicAgentTool.class.getName());
            }

            //依赖注入
            dependencyInjection(instance);

            TOOL_OBJ_CACHE.putIfAbsent(codeIdentity(codeSource), (IDynamicAgentTool) instance);

            return (IDynamicAgentTool) instance;
        }

        throw new IllegalArgumentException("loadNewInstance 执行失败, Glue 脚本为空");
    }

    @Override
    public CodeLanguage getLanguage() {
        return CodeLanguage.JAVA;
    }
}
