package com.hxh.apboa.core.hook;

import io.agentscope.core.hook.Hook;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 描述：代理钩子
 *
 * @author huxuehao
 **/
public interface IAgentHook extends Hook, SmartInitializingSingleton {
    default String getName() {
        return this.getClass().getSimpleName();
    }

    String getDescription();

    default void afterSingletonsInstantiated() {
        HooksRegister.register(this.getClass().getName(), this);
    };
}
