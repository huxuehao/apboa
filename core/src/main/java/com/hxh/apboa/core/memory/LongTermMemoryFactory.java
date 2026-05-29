package com.hxh.apboa.core.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.enums.LongTermMemoryType;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 长期记忆工厂
 * <p>
 * 使用策略模式管理多种长期记忆实现（Mem0、ReMe、百炼等）。
 * 根据 AgentDefinition 中配置的 memoryType 动态选择对应的 Builder 构建 LongTermMemory 实例。
 * 用户标识从 AgentContext 中自动获取，无需在 agent 配置中指定。
 *
 * @author wei.liu
 */
@Component
public class LongTermMemoryFactory {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryFactory.class);

    private final Map<LongTermMemoryType, LongTermMemoryBuilder> builderMap;

    public LongTermMemoryFactory(List<LongTermMemoryBuilder> builders) {
        this.builderMap = builders.stream()
                .collect(Collectors.toMap(
                        this::resolveBuilderType,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        log.info("长期记忆工厂初始化完成，已注册 {} 种记忆类型: {}", builderMap.size(), builderMap.keySet());
    }

    /**
     * 根据 AgentDefinition 创建 LongTermMemory
     *
     * @param definition Agent 定义
     * @return LongTermMemory 实例，如果未启用或配置无效则返回 null
     */
    public LongTermMemory createLongTermMemory(AgentDefinition definition) {
        if (definition == null || !Boolean.TRUE.equals(definition.getEnableLongTermMemory())) {
            return null;
        }

        JsonNode config = definition.getLongTermMemoryConfig();
        if (config == null) {
            log.warn("长期记忆已启用但未配置，agentId={}", definition.getId());
            return null;
        }

        LongTermMemoryType memoryType = resolveMemoryType(config);
        if (memoryType == null) {
            log.warn("无法识别的长期记忆类型，agentId={}", definition.getId());
            return null;
        }

        LongTermMemoryBuilder builder = builderMap.get(memoryType);
        if (builder == null) {
            log.warn("未找到 {} 类型的长期记忆构建器，agentId={}", memoryType, definition.getId());
            return null;
        }

        // 从嵌套格式中提取当前类型对应的子配置
        JsonNode typeConfig = resolveTypeConfig(config, memoryType);
        if (typeConfig == null) {
            log.warn("未找到 {} 类型的长期记忆配置，agentId={}", memoryType, definition.getId());
            return null;
        }

        // 从 AgentContext 获取用户信息
        AgentContext context = AgentContext.getIfExists().orElse(null);

        return builder.build(definition, typeConfig, context);
    }

    /**
     * 获取 LongTermMemoryMode
     *
     * @param definition Agent 定义
     * @return LongTermMemoryMode，如果未配置则返回 null
     */
    public LongTermMemoryMode getMemoryMode(AgentDefinition definition) {
        if (definition == null || !Boolean.TRUE.equals(definition.getEnableLongTermMemory())) {
            return null;
        }

        JsonNode config = definition.getLongTermMemoryConfig();
        if (config == null) {
            return null;
        }

        LongTermMemoryType memoryType = resolveMemoryType(config);
        if (memoryType == null) {
            return null;
        }

        LongTermMemoryBuilder builder = builderMap.get(memoryType);
        if (builder == null) {
            return null;
        }

        // 从嵌套格式中提取当前类型对应的子配置
        JsonNode typeConfig = resolveTypeConfig(config, memoryType);
        if (typeConfig == null) {
            return null;
        }

        return builder.getMemoryMode(typeConfig);
    }

    /**
     * 从配置中解析记忆类型
     */
    private LongTermMemoryType resolveMemoryType(JsonNode config) {
        if (config == null || !config.has("memoryType")) {
            return LongTermMemoryType.MEM0;
        }

        String typeStr = config.get("memoryType").asText();
        try {
            return LongTermMemoryType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从嵌套格式中提取指定类型的子配置
     * <p>
     * 新版嵌套格式：{ "memoryType": "MEM0", "MEM0": {...}, "REME": {...}, "BAILIAN": {...} }
     * 从 config 中提取 memoryType 对应的子配置对象
     */
    private JsonNode resolveTypeConfig(JsonNode config, LongTermMemoryType memoryType) {
        if (config == null || memoryType == null) {
            return null;
        }
        // 新版嵌套格式：通过 memoryType 字段获取当前类型，然后取对应 key 的子配置
        if (config.has(memoryType.name())) {
            return config.get(memoryType.name());
        }
        return null;
    }

    /**
     * 根据 Builder 实例解析对应的记忆类型
     */
    private LongTermMemoryType resolveBuilderType(LongTermMemoryBuilder builder) {
        if (builder instanceof Mem0LongTermMemoryBuilder) {
            return LongTermMemoryType.MEM0;
        } else if (builder instanceof ReMeLongTermMemoryBuilder) {
            return LongTermMemoryType.REME;
        } else if (builder instanceof BailianLongTermMemoryBuilder) {
            return LongTermMemoryType.BAILIAN;
        }
        throw new IllegalArgumentException("未知的 LongTermMemoryBuilder 类型: " + builder.getClass().getName());
    }
}
