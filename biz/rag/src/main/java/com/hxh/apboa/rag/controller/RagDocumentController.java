package com.hxh.apboa.rag.controller;

import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.entity.RagDocument;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.enums.RagDocumentStatus;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.rag.LocalRagService;
import com.hxh.apboa.core.rag.DocumentParser;
import com.hxh.apboa.core.rag.RagRepository;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.hxh.apboa.resource.service.AttachService;
import com.hxh.apboa.common.entity.Attach;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
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
    private final RagRepository ragRepository;
    private final KnowledgeBaseConfigService knowledgeBaseConfigService;
    private final AttachService attachService;

    /**
     * 上传文档到指定知识库
     */
    @PostMapping("/upload")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Long> upload(@RequestParam("file") MultipartFile file,
                          @RequestParam("knowledgeBaseConfigId") Long kbConfigId) {
        KnowledgeBaseConfig kbConfig = knowledgeBaseConfigService.getById(kbConfigId);
        if (kbConfig == null) {
            return R.fail("知识库配置不存在");
        }
        if (kbConfig.getKbType() != KbType.LOCAL) {
            return R.fail("仅支持本地类型知识库的文档上传");
        }

        try {
            String fileName = file.getOriginalFilename();
            if (!documentParser.isSupported(fileName)) {
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

            ragRepository.insertDocument(document);

            try (InputStream inputStream = file.getInputStream()) {
                localRagService.processDocument(document, inputStream, kbConfig);
            }

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
        List<RagDocument> documents = ragRepository.getDocumentsByKbConfigId(kbConfigId);
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
        return R.data(ragRepository.getChunksByDocumentId(documentId));
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

        List<com.hxh.apboa.common.entity.RagDocumentChunk> chunks =
                localRagService.retrieve(query, kbConfig, limit, scoreThreshold);

        List<Map<String, Object>> results = chunks.stream().map(chunk -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", chunk.getId());
            map.put("documentId", chunk.getDocumentId());
            map.put("chunkIndex", chunk.getChunkIndex());
            map.put("content", chunk.getContent());
            map.put("tokenCount", chunk.getTokenCount());
            return map;
        }).toList();

        return R.data(results);
    }

    private String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
