package com.hxh.apboa.core.tool.dynamices;

import com.hxh.apboa.common.enums.CodeLanguage;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 描述：实例加载接口
 *
 * @author huxuehao
 **/
public interface InstanceLoader extends SmartInitializingSingleton {

    /**
     * 加载实例
     * @param codeSource 源代码
     * @return 实例
     */
    IDynamicAgentTool loadInstance(String codeSource);

    /**
     * 获取语言
     * @return 语言
     */
    CodeLanguage getLanguage();

    /**
     * 初始化
     */
    @Override
    default void afterSingletonsInstantiated() {
        // 完成注册
        InstanceLoadFactory.registerLoader(this);
    };
}
