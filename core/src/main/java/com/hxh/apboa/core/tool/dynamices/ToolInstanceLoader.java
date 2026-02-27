package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.core.InstanceLoader;

/**
 * 描述：实例加载接口
 *
 * @author huxuehao
 **/
public interface ToolInstanceLoader extends InstanceLoader<IDynamicAgentTool> {
    /**
     * 初始化
     */
    @Override
    default void afterSingletonsInstantiated() {
        // 完成注册
        ToolInstanceLoadFactory.registerLoader(this);
    };
}
