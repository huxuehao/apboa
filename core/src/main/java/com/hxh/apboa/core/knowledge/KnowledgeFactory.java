package com.hxh.apboa.core.knowledge;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.wrapper.KnowledgeWrapper;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：知识库工程
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class KnowledgeFactory {
    private static final Map<KbType, IKnowledge> KNOWLEDGE_MAP = new ConcurrentHashMap<>();

    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public KnowledgeWrapper getKnowledge(AgentDefinition definition) {

        KnowledgeBaseConfig knowledgeBaseConfig = knowledgeBaseConfigService.getByAgentId(definition.getId());
        if (knowledgeBaseConfig == null) {
            return null;
        }

        if (!knowledgeBaseConfig.getEnabled()) {
            return null;
        }

        IKnowledge iKnowledge = KNOWLEDGE_MAP.get(knowledgeBaseConfig.getKbType());
        if (iKnowledge == null) {
            return null;
        }

        return KnowledgeWrapper
                .builder()
                .ragMode(knowledgeBaseConfig.getRagMode())
                .knowledge(iKnowledge.build(knowledgeBaseConfig))
                .build();
    }

    public static void register(IKnowledge knowledge) {
        KNOWLEDGE_MAP.put(knowledge.type(), knowledge);
    }

    public static void unregister(KbType type) {
        KNOWLEDGE_MAP.remove(type);
    }

    public static void clear() {
        KNOWLEDGE_MAP.clear();
    }
}
