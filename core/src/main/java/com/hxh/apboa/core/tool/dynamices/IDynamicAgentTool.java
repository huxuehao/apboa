package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.common.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 描述：动态代理工具接口
 *
 * @author huxuehao
 **/
public interface IDynamicAgentTool {
    /**
     * 执行
     */
    Object execute(Object ...args);

    /**
     *  将对象转换为json字符串
     */
    default String toJsonString(Object obj) {
        return JsonUtils.toJsonStr(obj);
    }

    /**
     *  将字符串解析为json节点
     */
    default JsonNode parse(String string) {
        return JsonUtils.parse(string);
    }

    /**
     *  将字符串解析为指定类的实例
     */
    default <T> T parse(String string, Class<T> clazz) {
        return JsonUtils.parse(string, clazz);
    }
}
