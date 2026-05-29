package com.hxh.apboa.core.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.AccountVO;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.memory.LongTermMemory;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.memory.mem0.Mem0ApiType;
import io.agentscope.core.memory.mem0.Mem0LongTermMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mem0 长期记忆构建器
 * <p>
 * 构建 Mem0LongTermMemory 实例。
 * 用户标识从 AgentContext 中自动获取（优先使用 nickname，其次 username，最后 id）。
 * apiType 从配置中读取，由用户自行选择部署类型（Platform / Self-hosted）。
 *
 * @author wei.liu
 */
@Component
public class Mem0LongTermMemoryBuilder implements LongTermMemoryBuilder {

    private static final Logger log = LoggerFactory.getLogger(Mem0LongTermMemoryBuilder.class);

    @Override
    public LongTermMemory build(AgentDefinition definition, JsonNode config, AgentContext context) {
        log.info("长期记忆[Mem0]: 创建 Mem0LongTermMemory, agentId={}, agentCode={}",
                definition.getId(), definition.getAgentCode());

        Mem0LongTermMemory.Builder builder = Mem0LongTermMemory.builder();

        // agentName: 使用 agentCode
        builder.agentName(definition.getAgentCode());

        // userId: 从 AgentContext 获取登录用户信息
        String userId = resolveUserId(context);
        builder.userId(userId);

        // apiBaseUrl: Mem0 服务地址
        String apiBaseUrl = JsonUtils.getStringValue(config, "apiBaseUrl", "https://api.mem0.ai");
        builder.apiBaseUrl(apiBaseUrl);

        // apiKey: Mem0 API 密钥
        String apiKey = JsonUtils.getStringValue(config, "apiKey", "");
        if (!apiKey.isEmpty()) {
            builder.apiKey(apiKey);
        }

        // apiType: 从配置中读取，用户自行选择部署类型
        String apiTypeStr = JsonUtils.getStringValue(config, "apiType", "platform");
        builder.apiType(Mem0ApiType.fromString(apiTypeStr));

        return builder.build();
    }

    @Override
    public LongTermMemoryMode getMemoryMode(JsonNode config) {
        if (config == null) {
            return LongTermMemoryMode.BOTH;
        }

        String mode = config.has("memoryMode") ? config.get("memoryMode").asText("BOTH") : "BOTH";
        try {
            return LongTermMemoryMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return LongTermMemoryMode.BOTH;
        }
    }

    /**
     * 从 AgentContext 中解析用户标识
     * 优先级：nickname > username > id
     */
    private String resolveUserId(AgentContext context) {
        if (context == null) {
            return "default_user";
        }
        AccountVO userInfo = context.getUserInfo();
        if (userInfo == null) {
            return "default_user";
        }
        if (userInfo.getNickname() != null && !userInfo.getNickname().isEmpty()) {
            return userInfo.getNickname();
        }
        if (userInfo.getUsername() != null && !userInfo.getUsername().isEmpty()) {
            return userInfo.getUsername();
        }
        return String.valueOf(userInfo.getId());
    }
}
