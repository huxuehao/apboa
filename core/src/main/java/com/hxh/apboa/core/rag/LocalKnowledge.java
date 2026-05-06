package com.hxh.apboa.core.rag;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.vo.RagDocumentChunkVO;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.model.RetrieveConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地RAG知识库实现，实现agentscope的Knowledge接口
 *
 * @author huxuehao
 */
public class LocalKnowledge implements Knowledge {

    private static final Logger log = LoggerFactory.getLogger(LocalKnowledge.class);

    private final Long knowledgeBaseConfigId;
    private final LocalRagService localRagService;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public LocalKnowledge(Long knowledgeBaseConfigId,
                          LocalRagService localRagService,
                          KnowledgeBaseConfigService knowledgeBaseConfigService) {
        this.knowledgeBaseConfigId = knowledgeBaseConfigId;
        this.localRagService = localRagService;
        this.knowledgeBaseConfigService = knowledgeBaseConfigService;
    }

    @Override
    public Mono<Void> addDocuments(List<Document> documents) {
        return Mono.fromRunnable(() -> {
            log.info("LocalKnowledge.addDocuments 被调用, 文档数量={}", documents.size());
        });
    }

    @Override
    public Mono<List<Document>> retrieve(String query, RetrieveConfig config) {
        return Mono.fromSupplier(() -> {
            try {
                KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(knowledgeBaseConfigId);
                if (kbConfig == null || !kbConfig.getEnabled()) {
                    log.warn("知识库配置不存在或已禁用, id={}", knowledgeBaseConfigId);
                    return new ArrayList<Document>();
                }

                int limit = config != null ? config.getLimit() : 5;
                double scoreThreshold = config != null ? config.getScoreThreshold() : 0.5;

                List<RagDocumentChunkVO> chunks = localRagService.retrieve(
                        query, kbConfig, limit, scoreThreshold);

                List<Document> documents = new ArrayList<>();
                for (RagDocumentChunkVO chunk : chunks) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("documentId", chunk.getDocumentId());
                    payload.put("chunkIndex", chunk.getChunkIndex());
                    payload.put("tokenCount", chunk.getTokenCount());

                    DocumentMetadata metadata = DocumentMetadata.builder()
                            .content(TextBlock.builder().text(chunk.getContent()).build())
                            .docId(String.valueOf(chunk.getDocumentId()))
                            .chunkId(String.valueOf(chunk.getId()))
                            .payload(payload)
                            .build();

                    Document doc = new Document(metadata);
                    documents.add(doc);
                }

                log.debug("本地RAG检索完成, query={}, results={}", query, documents.size());
                return documents;
            } catch (Exception e) {
                log.error("本地RAG检索失败, query={}", query, e);
                return new ArrayList<Document>();
            }
        });
    }
}
