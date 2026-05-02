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

        String workspaceTagExplanation = """
                ===================================================
                用户可以通过 <workspace-file>文件名</workspace-file> 标签引用工作空间中的文件。当你看到此标签时，应将其视为指示：前往当前工作空间查找对应的文件，并读取其内容以辅助回答或执行任务。
                """;
        prompt = prompt + "\n\n" + workspaceTagExplanation;

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
