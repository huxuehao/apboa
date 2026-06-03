package com.hxh.apboa.core.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.memory.LongTermMemoryMode;

/**
 * 长期记忆构建器策略接口
 * <p>
 * 每种长期记忆类型（Mem0、ReMe、百炼等）实现此接口，
 * 通过策略模式实现可插拔的长期记忆构建。
 *
 * @author wei.liu
 */
public interface LongTermMemoryBuilder {

    /**
     * 根据 AgentDefinition 构建 LongTermMemory 实例
     *
     * @param definition Agent 定义
     * @param config     长期记忆配置（JSON格式）
     * @param context    Agent 上下文（用于获取用户信息等）
     * @return LongTermMemory 实例
     */
    LongTermMemory build(AgentDefinition definition, JsonNode config, AgentContext context);

    /**
     * 获取 LongTermMemoryMode
     *
     * @param config 长期记忆配置
     * @return LongTermMemoryMode，如果未配置则返回默认值
     */
    LongTermMemoryMode getMemoryMode(JsonNode config);
}
