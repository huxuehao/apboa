package com.hxh.apboa.core.prompt.impl;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SystemPromptTemplate;
import com.hxh.apboa.core.prompt.AgentSysPrompt;
import com.hxh.apboa.prompt.service.SystemPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 描述：默认系统提示词实现
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class DefaultAgentSysPrompt implements AgentSysPrompt {
    private final SystemPromptTemplateService systemPromptTemplateService;

    @Override
    public String getPrompt(AgentDefinition agentDefinition) {
        if (agentDefinition.getFollowTemplate() != null && agentDefinition.getFollowTemplate()) {
            SystemPromptTemplate promptTemplate = systemPromptTemplateService.getById(agentDefinition.getSystemPromptTemplateId());
            if (promptTemplate == null) {
                throw new RuntimeException("System prompt template not found");
            }

            if (promptTemplate.getEnabled()) {
                return promptTemplate.getContent();
            }

            throw new RuntimeException("System prompt template is disabled");
        } else {
            return agentDefinition.getSystemPrompt();
        }
    }

    @Override
    public int order() {
        return 0;
    }
}
