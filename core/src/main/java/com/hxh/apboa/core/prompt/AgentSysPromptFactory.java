package com.hxh.apboa.core.prompt;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.hxh.apboa.sensitive.service.SensitiveWordConfigService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：提示词工厂
 *
 * @author huxuehao
 **/
@Component
public class AgentSysPromptFactory {
    private final AgentSysPrompt primaryAgentSysPrompt;
    private final SensitiveWordConfigService sensitiveWordConfigService;

    public AgentSysPromptFactory(List<AgentSysPrompt> implementations, SensitiveWordConfigService sensitiveWordConfigService) {
        this.sensitiveWordConfigService = sensitiveWordConfigService;
        // 降序
        implementations.sort((o1, o2) -> o2.order() - o1.order());
        // 获取优先级最高的实现
        this.primaryAgentSysPrompt = implementations.getFirst();
    }

    public String getAgentSysPrompt(AgentDefinition agentDefinition) {
        String prompt = primaryAgentSysPrompt.getPrompt(agentDefinition);

        Long id = agentDefinition.getSensitiveWordConfigId();
        if (id == null) {
            return prompt;
        }

        SensitiveWordConfig sensitiveWord = sensitiveWordConfigService.getById(id);
        if (sensitiveWord == null) {
            return prompt;
        }

        List<String> words = new ArrayList<>();
        sensitiveWord.getWords().forEach(word -> {
            words.add(word.asText());
        });

        return SensitiveWordHelper.fillSensitiveWordToPrompt(words, prompt);
    }
}
