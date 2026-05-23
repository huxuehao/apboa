package com.hxh.apboa.rag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.entity.RagDocument;
import com.hxh.apboa.common.entity.RagDocumentChunk;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.enums.RagDocumentStatus;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.vo.RagDocumentChunkVO;
import com.hxh.apboa.core.rag.DocumentParser;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.hxh.apboa.core.rag.mapper.RagDocumentChunkMapper;
import com.hxh.apboa.core.rag.mapper.RagDocumentMapper;
import com.hxh.apboa.core.rag.service.LocalRagService;
import com.hxh.apboa.resource.service.AttachService;
import com.hxh.apboa.common.entity.Attach;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG文档管理Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/rag/document")
@RequiredArgsConstructor
public class RagDocumentController {

    private final LocalRagService localRagService;
    private final DocumentParser documentParser;
    private final RagDocumentMapper ragDocumentMapper;
    private final RagDocumentChunkMapper ragDocumentChunkMapper;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;
    private final AttachService attachService;

    /**
     * 上传文档到指定知识库
     */
    @PostMapping("/upload")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Long> upload(@RequestParam("file") MultipartFile file,
                          @RequestParam("knowledgeBaseConfigId") Long kbConfigId,
                          @RequestParam(value = "parserType", required = false) String parserType,
                          @RequestParam(value = "chunkStrategy", required = false) String chunkStrategy,
                          @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
                          @RequestParam(value = "overlap", required = false) Integer overlap,
                          @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap,
                          @RequestParam(value = "chunkDelimiters", required = false) String chunkDelimiters,
                          @RequestParam(value = "separators", required = false) String separators) {
        KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(kbConfigId);
        if (kbConfig == null) {
            return R.fail("知识库配置不存在");
        }
        if (kbConfig.getKbType() != KbType.LOCAL) {
            return R.fail("仅支持本地类型知识库的文档上传");
        }

        try {
            String fileName = file.getOriginalFilename();
            if (documentParser.isNotSupported(fileName)) {
                return R.fail("不支持的文件类型，支持的格式: txt、md、pdf、doc、docx、xlsx、xls、csv、pptx、ppt");
            }

            Attach attach = attachService.upload(file, fileName);
            String fileType = extractFileType(fileName);

            RagDocument document = RagDocument.builder()
                    .id(IdWorker.getId())
                    .knowledgeBaseConfigId(kbConfigId)
                    .fileName(fileName)
                    .filePath(String.valueOf(attach.getId()))
                    .fileSize(file.getSize())
                    .fileType(fileType)
                    .chunkCount(0)
                    .status(RagDocumentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            ragDocumentMapper.insert(document);

            // 异步处理文档，从已保存的附件中读取文件流
            localRagService.reprocessDocument(document, attach,
                    mergeProcessingOptions(kbConfig, parserType, chunkStrategy, chunkSize, overlap, chunkOverlap, chunkDelimiters, separators));

            return R.data(document.getId());
        } catch (Exception e) {
            return R.fail("文档上传处理失败: " + e.getMessage());
        }
    }

    /**
     * 查询知识库下的文档列表
     */
    @GetMapping("/list")
    public R<List<RagDocument>> list(@RequestParam("knowledgeBaseConfigId") Long kbConfigId) {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getKnowledgeBaseConfigId, kbConfigId)
                .orderByDesc(RagDocument::getCreatedAt);
        List<RagDocument> documents = ragDocumentMapper.selectList(wrapper);
        return R.data(documents);
    }

    /**
     * 删除文档
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            localRagService.deleteDocument(id);
        }
        return R.data(true);
    }

    /**
     * 查询文档分块列表
     */
    @GetMapping("/chunks")
    public R<List<com.hxh.apboa.common.entity.RagDocumentChunk>> chunks(
            @RequestParam("documentId") Long documentId) {
        LambdaQueryWrapper<RagDocumentChunk> chunkWrapper = new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getDocumentId, documentId)
                .orderByAsc(RagDocumentChunk::getChunkIndex);
        return R.data(ragDocumentChunkMapper.selectList(chunkWrapper));
    }

    /**
     * 更新分块内容
     */
    @PutMapping("/chunk/{id}")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> updateChunk(@PathVariable("id") Long chunkId,
                                   @RequestBody Map<String, String> params) {
        String content = params.get("content");
        if (content == null || content.isBlank()) {
            return R.fail("分块内容不能为空");
        }
        try {
            localRagService.updateChunk(chunkId, content);
            return R.data(true);
        } catch (Exception e) {
            return R.fail("更新分块失败: " + e.getMessage());
        }
    }

    /**
     * 删除分块
     */
    @DeleteMapping("/chunk/{id}")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> deleteChunk(@PathVariable("id") Long chunkId) {
        try {
            localRagService.deleteChunk(chunkId);
            return R.data(true);
        } catch (Exception e) {
            return R.fail("删除分块失败: " + e.getMessage());
        }
    }

    /**
     * RAG检索测试
     */
    @PostMapping("/search")
    public R<List<Map<String, Object>>> search(@RequestBody Map<String, Object> params) {
        Long kbConfigId = Long.valueOf(params.get("knowledgeBaseConfigId").toString());
        String query = (String) params.get("query");
        int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit").toString()) : 5;
        double scoreThreshold = params.containsKey("scoreThreshold")
                ? Double.parseDouble(params.get("scoreThreshold").toString()) : 0.5;

        KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(kbConfigId);
        if (kbConfig == null) {
            return R.fail("知识库配置不存在");
        }

        List<RagDocumentChunkVO> chunks =
                localRagService.retrieve(query, kbConfig, limit, scoreThreshold);

        List<Map<String, Object>> results = chunks.stream().map(chunk -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", chunk.getId());
            map.put("documentId", chunk.getDocumentId());
            map.put("fileName", chunk.getFileName());
            map.put("chunkIndex", chunk.getChunkIndex());
            map.put("content", chunk.getContent());
            map.put("tokenCount", chunk.getTokenCount());
            map.put("score", chunk.getScore());
            return map;
        }).toList();

        return R.data(results);
    }



    /**
     * 下载文档原始文件
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        RagDocument document = ragDocumentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        try {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;filename=" + URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8));
            Attach attach = attachService.getById(Long.valueOf(document.getFilePath()));
            if (attach == null) {
                throw new RuntimeException("文件附件不存在");
            }
            try (OutputStream outputStream = response.getOutputStream()) {
                attachService.download(attach, outputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败", e);
        }
    }

    /**
     * 重新上传文档（替换原有文件并重新处理）
     */
    @PostMapping("/re-upload/{id}")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> reUpload(@PathVariable("id") Long id,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam(value = "parserType", required = false) String parserType,
                               @RequestParam(value = "chunkStrategy", required = false) String chunkStrategy,
                               @RequestParam(value = "chunkSize", required = false) Integer chunkSize,
                               @RequestParam(value = "overlap", required = false) Integer overlap,
                               @RequestParam(value = "chunkOverlap", required = false) Integer chunkOverlap,
                               @RequestParam(value = "chunkDelimiters", required = false) String chunkDelimiters,
                               @RequestParam(value = "separators", required = false) String separators) {
        RagDocument document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return R.fail("文档不存在");
        }

        KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(document.getKnowledgeBaseConfigId());
        if (kbConfig == null) {
            return R.fail("知识库配置不存在");
        }

        try {
            String fileName = file.getOriginalFilename();
            if (documentParser.isNotSupported(fileName)) {
                return R.fail("不支持的文件类型");
            }

            // 删除旧的向量和分块数据
            localRagService.deleteDocumentChunksAndVectors(id);

            // 删除旧附件并上传新附件
            Attach oldAttach = attachService.getById(Long.valueOf(document.getFilePath()));
            if (oldAttach != null) {
                attachService.removeById(oldAttach.getId());
            }

            Attach newAttach = attachService.upload(file, fileName);

            // 更新文档记录
            document.setFileName(fileName);
            document.setFilePath(String.valueOf(newAttach.getId()));
            document.setFileSize(file.getSize());
            document.setFileType(extractFileType(fileName));
            document.setChunkCount(0);
            document.setStatus(RagDocumentStatus.PENDING);
            document.setErrorMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);

            // 异步重新处理文档，从已保存的附件中读取文件流
            localRagService.reprocessDocument(document, newAttach,
                    mergeProcessingOptions(kbConfig, parserType, chunkStrategy, chunkSize, overlap, chunkOverlap, chunkDelimiters, separators));

            return R.data(true);
        } catch (Exception e) {
            return R.fail("重新上传处理失败: " + e.getMessage());
        }
    }

    /**
     * 重新分块处理（使用当前知识库配置重新解析和向量化）
     */
    @PostMapping("/re-chunk/{id}")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> reChunk(@PathVariable("id") Long id,
                              @RequestBody(required = false) Map<String, Object> params) {
        RagDocument document = ragDocumentMapper.selectById(id);
        if (document == null) {
            return R.fail("文档不存在");
        }

        KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(document.getKnowledgeBaseConfigId());
        if (kbConfig == null) {
            return R.fail("知识库配置不存在");
        }

        try {
            // 删除旧的向量和分块数据
            localRagService.deleteDocumentChunksAndVectors(id);

            // 通过附件服务获取文件流并重新处理
            Attach attach = attachService.getById(Long.valueOf(document.getFilePath()));
            if (attach == null) {
                return R.fail("文件附件不存在，请重新上传");
            }

            document.setChunkCount(0);
            document.setStatus(RagDocumentStatus.PROCESSING);
            document.setErrorMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);

            // 异步重新处理文档
            localRagService.reprocessDocument(document, attach,
                    mergeProcessingOptions(
                            kbConfig,
                            getString(params, "parserType"),
                            getString(params, "chunkStrategy"),
                            getInteger(params, "chunkSize"),
                            getInteger(params, "overlap"),
                            getInteger(params, "chunkOverlap"),
                            getString(params, "chunkDelimiters"),
                            getSeparators(params)
                    ));

            return R.data(true);
        } catch (Exception e) {
            document.setStatus(RagDocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            document.setUpdatedAt(LocalDateTime.now());
            ragDocumentMapper.updateById(document);
            return R.fail("重新分块失败: " + e.getMessage());
        }
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private KnowledgeBaseConfig mergeProcessingOptions(KnowledgeBaseConfig source,
                                                       String parserType,
                                                       String chunkStrategy,
                                                       Integer chunkSize,
                                                       Integer overlap,
                                                       Integer chunkOverlap,
                                                       String chunkDelimiters,
                                                       String separators) {
        KnowledgeBaseConfig copy = new KnowledgeBaseConfig();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setKbType(source.getKbType());
        copy.setRagMode(source.getRagMode());
        copy.setDescription(source.getDescription());
        copy.setConnectionConfig(source.getConnectionConfig());
        copy.setEndpointConfig(source.getEndpointConfig());
        copy.setRerankingConfig(source.getRerankingConfig());
        copy.setQueryRewriteConfig(source.getQueryRewriteConfig());
        copy.setMetadataFilters(source.getMetadataFilters());
        copy.setHttpConfig(source.getHttpConfig());
        copy.setHealthStatus(source.getHealthStatus());
        copy.setLastSyncTime(source.getLastSyncTime());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setUpdatedBy(source.getUpdatedBy());

        Map<String, Object> retrieval = new HashMap<>();
        JsonNode current = source.getRetrievalConfig();
        if (current != null && !current.isNull()) {
            Map<String, Object> existing = JsonUtils.parse(current.toString(), Map.class);
            if (existing != null) {
                retrieval.putAll(existing);
            }
        }

        putIfHasText(retrieval, "parserType", parserType);
        putIfHasText(retrieval, "chunkStrategy", chunkStrategy);
        putIfPresent(retrieval, "chunkSize", chunkSize);
        putIfPresent(retrieval, "overlap", overlap);
        putIfPresent(retrieval, "chunkOverlap", chunkOverlap != null ? chunkOverlap : overlap);
        putIfHasText(retrieval, "chunkDelimiters", chunkDelimiters);
        if (separators != null && !separators.isBlank()) {
            retrieval.put("separators", separators.split(","));
        }

        copy.setRetrievalConfig(JsonUtils.toJsonNode(retrieval));
        return copy;
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    private void putIfPresent(Map<String, Object> target, String key, Integer value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String getString(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Integer getInteger(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String getSeparators(Map<String, Object> params) {
        if (params == null) {
            return null;
        }
        Object value = params.get("separators");
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse(null);
        }
        return getString(params, "separators");
    }
}
