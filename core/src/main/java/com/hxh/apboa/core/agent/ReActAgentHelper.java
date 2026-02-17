package com.hxh.apboa.core.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.wrapper.KnowledgeWrapper;
import com.hxh.apboa.core.hook.HooksFactory;
import com.hxh.apboa.core.knowledge.KnowledgeFactory;
import com.hxh.apboa.core.model.ChatModelFactory;
import com.hxh.apboa.core.prompt.AgentSysPromptFactory;
import com.hxh.apboa.core.skill.SkillBoxFactory;
import com.hxh.apboa.core.tool.ToolkitFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextHook;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.StatePersistence;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：智能体Helper
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class ReActAgentHelper {
    private final AgentDefinitionService agentDefinitionService;
    private final HooksFactory hooksFactory;
    private final ChatModelFactory chatModelFactory;
    private final AgentSysPromptFactory agentSysPromptFactory;
    private final SkillBoxFactory skillBoxFactory;
    private final ToolkitFactory toolkitFactory;
    private final KnowledgeFactory knowledgeFactory;

    /**
     * 获取 ReActAgent
     * @param agentId agentId
     */
    public ReActAgent getReActAgent(Long agentId) {
        AgentDefinition definition = agentDefinitionService.getById(agentId);
        if (definition == null) {
            throw new RuntimeException("Agent not found, agentId: " + agentId);
        }

        if (!definition.getEnabled()) {
            throw new RuntimeException("Agent is disabled, agentId: " + agentId);
        }

        return getReActAgent(definition);
    }
    /**
     * 获取预构建的 ReActAgent.Builder
     * @param agentId agentId
     */
    public ReActAgent.Builder getReActAgentBuilder(Long agentId) {
        AgentDefinition definition = agentDefinitionService.getById(agentId);
        if (definition == null) {
            throw new RuntimeException("Agent not found, agentId: " + agentId);
        }

        if (!definition.getEnabled()) {
            throw new RuntimeException("Agent is disabled, agentId: " + agentId);
        }

        return getReActAgentBuilder(definition);
    }

    /**
     * 获取 ReActAgent
     * @param definition agent 定义
     */
    public ReActAgent getReActAgent(AgentDefinition definition) {
        return getReActAgentBuilder(definition).build();
    }

    /**
     * 获取预构建的 ReActAgent.Builder
     * @param definition agent 定义
     */
    public ReActAgent.Builder getReActAgentBuilder(AgentDefinition definition) {
        Model model = chatModelFactory.getModel(definition);
        ReActAgent.Builder builder = ReActAgent.builder()
                .name(definition.getAgentCode())
                .description(FuncUtils.isEmpty(definition.getDescription()) ? definition.getName() : definition.getDescription())
                .maxIters(definition.getMaxIterations())
                .model(model)
                .sysPrompt(agentSysPromptFactory.getAgentSysPrompt(definition));

        SkillBox skillBox = skillBoxFactory.getSkillBox(definition);
        if (skillBox != null) {
            builder.skillBox(skillBox);
        }

        Toolkit toolkit = toolkitFactory.getToolkit(definition);
        if (toolkit != null) {
            builder.toolkit(toolkit);
        }

        KnowledgeWrapper knowledgeWrapper = knowledgeFactory.getKnowledge(definition);
        if (knowledgeWrapper != null) {
            builder.knowledge(knowledgeWrapper.getKnowledge());
            builder.ragMode(knowledgeWrapper.getRagMode());
            // builder.enableOnlyForUserQueries(true); // TODO
            builder.retrieveConfig(
                    RetrieveConfig.builder()
                            .limit(5)
                            .scoreThreshold(0.5)
                            .build());
        }

        if (definition.getEnablePlanning()) {
            PlanNotebook planNotebook = PlanNotebook.builder()
                    .maxSubtasks(definition.getMaxSubtasks())
                    .needUserConfirm(definition.getRequirePlanConfirmation())
                    .build();
            builder.planNotebook(planNotebook);
        }

        // 使用可变列表，避免 getHooks 返回 List.of() 时 add 抛 UnsupportedOperationException
        List<Hook> hooks = hooksFactory.getHooks(definition);
        hooks = hooks != null ? new ArrayList<>(hooks) : new ArrayList<>();

        // 配置记忆
        if (definition.getEnableMemory()) {
            if (definition.getEnableMemoryCompression()) {
                JsonNode config = definition.getMemoryCompressionConfig();
                AutoContextConfig autoContextConfig = AutoContextConfig.builder()
                        .maxToken(JsonUtils.getLongValue(config, "maxToken", 131072L))
                        .msgThreshold(JsonUtils.getIntValue(config, "msgThreshold", 100))
                        .lastKeep(JsonUtils.getIntValue(config, "lastKeep", 50))
                        .tokenRatio(JsonUtils.getDoubleValue(config, "tokenRatio", 0.75F))
                        .build();
                builder.memory(new AutoContextMemory(autoContextConfig, model));
                hooks.add(new AutoContextHook());
            } else {
                builder.memory(new InMemoryMemory());
            }
            // 启用会话持久化时，确保 Memory 可被 saveTo/loadFrom；若启用规划则同时持久化 PlanNotebook
            builder.statePersistence(
                    StatePersistence.builder()
                            .memoryManaged(true)
                            .planNotebookManaged(definition.getEnablePlanning())
                            .build());
        }

        // 添加Hook
        if (!hooks.isEmpty()) {
            builder.hooks(hooks);
        }

        // 结构化输出
        if (definition.getStructuredOutputEnabled()) {
            builder.structuredOutputReminder(definition.getStructuredOutputReminder());
        }

        return builder;
    }
}
