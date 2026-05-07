package com.hxh.apboa.core.rag.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.entity.Attach;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.entity.RagDocument;
import com.hxh.apboa.common.entity.RagDocumentChunk;
import com.hxh.apboa.common.enums.RagDocumentStatus;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.RagDocumentChunkVO;
import com.hxh.apboa.core.rag.DocumentParser;
import com.hxh.apboa.core.rag.EmbeddingService;
import com.hxh.apboa.core.rag.PgVectorStore;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.hxh.apboa.core.rag.mapper.RagDocumentChunkMapper;
import com.hxh.apboa.core.rag.mapper.RagDocumentMapper;
import com.hxh.apboa.core.rag.service.TextChunker.ChunkResult;
import com.hxh.apboa.resource.service.AttachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 本地RAG服务，编排文档解析、分块、向量化、存储的完整流程
 *
 * @author huxuehao
 */
@Component
public class LocalRagService {

    private static final Logger log = LoggerFactory.getLogger(LocalRagService.class);

    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final PgVectorStore pgVectorStore;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentChunkMapper ragDocumentChunkMapper;
    private final AttachService attachService;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;

    public LocalRagService(DocumentParser documentParser,
                           TextChunker textChunker,
                           EmbeddingService embeddingService,
                           PgVectorStore pgVectorStore,
                           RagDocumentMapper ragDocumentMapper,
                           RagDocumentChunkMapper ragDocumentChunkMapper,
                           AttachService attachService,
                           KnowledgeBaseConfigService knowledgeBaseConfigService) {
        this.documentParser = documentParser;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.pgVectorStore = pgVectorStore;
        this.ragDocumentMapper = ragDocumentMapper;
        this.ragDocumentChunkMapper = ragDocumentChunkMapper;
        this.attachService = attachService;
        this.knowledgeBaseConfigService = knowledgeBaseConfigService;
    }

    /**
     * 处理文档：解析 -> 分块 -> 向量化 -> 存储
     *
     * @param document   文档记录
     * @param inputStream 文件输入流
     * @param config     知识库配置
     */
    public void processDocument(RagDocument document, InputStream inputStream, KnowledgeBaseConfig config) {
        document.setStatus(RagDocumentStatus.PROCESSING);
        document.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(document);

        try {
            String rowDelimiter = getFirstChunkDelimiter(config);
            String text = documentParser.parse(inputStream, document.getFileName(), rowDelimiter);

            int chunkSize = getChunkSize(config);
            int chunkOverlap = getChunkOverlap(config);

            List<ChunkResult> chunks = doChunk(text, chunkSize, chunkOverlap, config);
            if (chunks.isEmpty()) {
                document.setStatus(RagDocumentStatus.FAILED);
                document.setErrorMessage("文档解析后内容为空");
                document.setUpdatedAt(LocalDateTime.now());
                ragDocumentMapper.updateById(document);
                return;
            }

            List<String> chunkTexts = chunks.stream().map(ChunkResult::content).toList();
            List<float[]> embeddings = embeddingService.embed(chunkTexts, config);

            List<RagDocumentChunk> chunkEntities = new ArrayList<>();
            List<PgVectorStore.EmbeddingRecord> embeddingRecords = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                ChunkResult chunk = chunks.get(i);

                RagDocumentChunk chunkEntity = RagDocumentChunk.builder()
                        .id(IdWorker.getId())
                        .documentId(document.getId())
                        .fileName(document.getFileName())
                        .chunkIndex(chunk.index())
                        .content(chunk.content())
                        .tokenCount(estimateTokenCount(chunk.content()))
                        .startOffset(chunk.startOffset())
                        .endOffset(chunk.endOffset())
                        .createdAt(LocalDateTime.now())
                        .build();
                chunkEntities.add(chunkEntity);

                if (i < embeddings.size()) {
                    embeddingRecords.add(new PgVectorStore.EmbeddingRecord(
                            chunkEntity.getId(),
                            chunkEntity.getId(),
                            document.getId(),
                            document.getKnowledgeBaseConfigId(),
                            embeddings.get(i)
                    ));
                }
            }

            for (RagDocumentChunk chunkEntity : chunkEntities) {
                ragDocumentChunkMapper.insert(chunkEntity);
            }
            pgVectorStore.storeEmbeddings(embeddingRecords);

            document.setChunkCount(chunks.size());
            document.setStatus(RagDocumentStatus.COMPLETED);
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);

            log.info("文档处理完成, docId={}, chunks={}", document.getId(), chunks.size());
        } catch (Exception e) {
            log.error("文档处理失败, docId={}", document.getId(), e);
            document.setStatus(RagDocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);
        }
    }

    /**
     * 检索相关文档分块
     *
     * @param query    查询文本
     * @param config   知识库配置
     * @param limit    返回数量
     * @param scoreThreshold 分数阈值
     * @return 相关文档分块列表
     */
    public List<RagDocumentChunkVO> retrieve(String query, KnowledgeBaseConfig config,
                                           int limit, double scoreThreshold) {
        float[] queryEmbedding = embeddingService.embed(query, config);

        List<PgVectorStore.RetrievalResult> results = pgVectorStore.search(
                queryEmbedding, config.getId(), limit, scoreThreshold);

        List<RagDocumentChunkVO> chunks = new ArrayList<>();
        for (PgVectorStore.RetrievalResult result : results) {
            RagDocumentChunk chunk = ragDocumentChunkMapper.selectById(result.chunkId());
            if (chunk != null) {
                RagDocumentChunkVO chunkVo = new RagDocumentChunkVO();
                BeanUtils.copyProperties(chunk, chunkVo);
                chunkVo.setScore(result.score());
                chunks.add(chunkVo);
            }
        }

        return chunks;
    }

    /**
     * 删除文档及其关联的向量数据
     */
    public void deleteDocument(Long documentId) {
        pgVectorStore.deleteByDocumentId(documentId);
        ragDocumentChunkMapper.delete(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getDocumentId, documentId));
        ragDocumentMapper.deleteById(documentId);
    }

    /**
     * 仅删除文档的分块和向量数据（不删除文档记录本身），用于重新分块场景
     */
    public void deleteDocumentChunksAndVectors(Long documentId) {
        pgVectorStore.deleteByDocumentId(documentId);
        ragDocumentChunkMapper.delete(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getDocumentId, documentId));
    }

    /**
     * 通过附件服务重新获取文件流并重新处理文档（重新分块场景）
     * 异步执行，不阻塞调用线程
     */
    @Async("ragDocExecutor")
    public void reprocessDocument(RagDocument document, Attach attach, KnowledgeBaseConfig config) {
        document.setStatus(RagDocumentStatus.PROCESSING);
        document.setUpdatedAt(LocalDateTime.now());
        ragDocumentMapper.updateById(document);

        try (InputStream inputStream = attachService.downloadAsStream(attach)) {
            processDocument(document, inputStream, config);
        } catch (Exception e) {
            log.error("重新处理文档失败, docId={}", document.getId(), e);
            document.setStatus(RagDocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);
        }
    }

    /**
     * 更新分块内容并重新向量化
     */
    public void updateChunk(Long chunkId, String newContent) {
        RagDocumentChunk chunk = ragDocumentChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new RuntimeException("分块不存在");
        }

        RagDocument document = ragDocumentMapper.selectById(chunk.getDocumentId());
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        KnowledgeBaseConfig config = knowledgeBaseConfigService.getById(document.getKnowledgeBaseConfigId());
        if (config == null) {
            throw new RuntimeException("知识库配置不存在");
        }

        try {
            int newTokenCount = estimateTokenCount(newContent);
            chunk.setContent(newContent);
            chunk.setTokenCount(newTokenCount);
            ragDocumentChunkMapper.updateById(chunk);

            float[] newEmbedding = embeddingService.embed(newContent, config);
            pgVectorStore.deleteByChunkId(chunkId);
            pgVectorStore.storeEmbedding(chunkId, chunkId, chunk.getDocumentId(),
                    document.getKnowledgeBaseConfigId(), newEmbedding);

            log.info("分块更新成功, chunkId={}", chunkId);
        } catch (Exception e) {
            log.error("分块更新失败, chunkId={}", chunkId, e);
            throw new RuntimeException("分块更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定分块及其向量数据
     */
    public void deleteChunk(Long chunkId) {
        RagDocumentChunk chunk = ragDocumentChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new RuntimeException("分块不存在");
        }

        try {
            ragDocumentChunkMapper.deleteById(chunkId);
            pgVectorStore.deleteByChunkId(chunkId);
            log.info("分块删除成功, chunkId={}", chunkId);
        } catch (Exception e) {
            log.error("分块删除失败, chunkId={}", chunkId, e);
            throw new RuntimeException("分块删除失败: " + e.getMessage(), e);
        }
    }

    private int getChunkSize(KnowledgeBaseConfig config) {
        JsonNode retrievalConfig = config.getRetrievalConfig();
        return JsonUtils.getIntValue(retrievalConfig, "chunkSize", 512);
    }

    private int getChunkOverlap(KnowledgeBaseConfig config) {
        JsonNode retrievalConfig = config.getRetrievalConfig();
        return JsonUtils.getIntValue(retrievalConfig, "chunkOverlap", 64);
    }

    /**
     * 根据配置执行分块，有分隔符时按分隔符分块，否则按固定大小分块
     */
    private List<ChunkResult> doChunk(String text, int chunkSize, int chunkOverlap, KnowledgeBaseConfig config) {
        List<String> delimiters = getChunkDelimiters(config);
        return textChunker.delimiterChunk(text, chunkSize, chunkOverlap, delimiters);
    }

    /**
     * 获取第一个分块分隔符，用于 Excel 等文档在解析时作为行分隔符
     */
    private String getFirstChunkDelimiter(KnowledgeBaseConfig config) {
        JsonNode retrievalConfig = config.getRetrievalConfig();
        String delimitersStr = JsonUtils.getStringValue(retrievalConfig, "chunkDelimiters", null);
        if (delimitersStr == null || delimitersStr.isEmpty()) {
            return null;
        }
        String first = Arrays.stream(delimitersStr.split(","))
                .map(String::trim)
                .filter(d -> !d.isEmpty())
                .findFirst()
                .orElse(null);
        return first != null ? unescapeDelimiter(first) : null;
    }

    private List<String> getChunkDelimiters(KnowledgeBaseConfig config) {
        JsonNode retrievalConfig = config.getRetrievalConfig();
        String delimitersStr = JsonUtils.getStringValue(retrievalConfig, "chunkDelimiters", null);
        if (delimitersStr == null || delimitersStr.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(delimitersStr.split(","))
                .map(String::trim)
                .map(this::unescapeDelimiter)
                .filter(d -> !d.isEmpty())
                .toList();
    }

    /**
     * 将转义字符还原为实际字符，例如 \\n -> \n, \\t -> \t
     */
    private String unescapeDelimiter(String delimiter) {
        return delimiter
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");
    }

    private int estimateTokenCount(String text) {
        return (int) (text.length() * 0.6);
    }
}
