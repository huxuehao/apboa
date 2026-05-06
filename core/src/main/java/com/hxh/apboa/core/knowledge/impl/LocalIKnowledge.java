package com.hxh.apboa.core.knowledge.impl;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.core.knowledge.IKnowledge;
import com.hxh.apboa.core.rag.knowledge.LocalKnowledge;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.hxh.apboa.core.rag.service.LocalRagService;
import io.agentscope.core.rag.Knowledge;
import org.springframework.stereotype.Component;

/**
 * 本地RAG知识库实现，集成到KnowledgeFactory
 *
 * @author huxuehao
 */
@Component
public class LocalIKnowledge implements IKnowledge {

    private final LocalRagService localRagService;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public LocalIKnowledge(LocalRagService localRagService,
                           KnowledgeBaseConfigService knowledgeBaseConfigService) {
        this.localRagService = localRagService;
        this.knowledgeBaseConfigService = knowledgeBaseConfigService;
    }

    @Override
    public Knowledge build(KnowledgeBaseConfig knowledgeBaseConfig) {
        return new LocalKnowledge(
                knowledgeBaseConfig.getId(),
                localRagService,
                knowledgeBaseConfigService
        );
    }

    @Override
    public KbType type() {
        return KbType.LOCAL;
    }
}
