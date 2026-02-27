package com.hxh.apboa.core.hook.dynamices;

import com.hxh.apboa.core.InstanceLoader;
import io.agentscope.core.hook.Hook;

/**
 * 描述：HookInstanceLoader
 *
 * @author huxuehao
 **/
public interface HookInstanceLoader extends InstanceLoader<Hook> {
    /**
     * 初始化
     */
    @Override
    default void afterSingletonsInstantiated() {
        // 完成注册
        HookInstanceLoadFactory.registerLoader(this);
    };
}
