package com.hxh.apboa.core.prompt;

import com.hxh.apboa.common.entity.AgentDefinition;

/**
 * 描述：agent 系统提示词
 *
 * @author huxuehao
 **/
public interface AgentSysPrompt {
    String getPrompt(AgentDefinition agentDefinition);

    /**
     * 获取顺序（越大优先级越高）
     *
     * @return 顺序
     */
    int order();
}
