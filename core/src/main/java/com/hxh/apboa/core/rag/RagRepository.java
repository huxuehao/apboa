package com.hxh.apboa.core.rag;

import com.hxh.apboa.common.entity.RagDocument;
import com.hxh.apboa.common.entity.RagDocumentChunk;
import com.hxh.apboa.common.enums.RagDocumentStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG数据仓库，封装对MySQL中RAG文档和分块表的CRUD操作
 *
 * @author huxuehao
 */
@Component
public class RagRepository {

    private final JdbcTemplate jdbcTemplate;

    public RagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertDocument(RagDocument document) {
        jdbcTemplate.update(
                "INSERT INTO rag_document (id, knowledge_base_config_id, file_name, file_path, " +
                        "file_size, file_type, chunk_count, status, error_message, created_at, updated_at, created_by, updated_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                document.getId(), document.getKnowledgeBaseConfigId(), document.getFileName(),
                document.getFilePath(), document.getFileSize(), document.getFileType(),
                document.getChunkCount(), document.getStatus().name(), document.getErrorMessage(),
                document.getCreatedAt(), document.getUpdatedAt(), document.getCreatedBy(), document.getUpdatedBy()
        );
    }

    public void updateDocument(RagDocument document) {
        jdbcTemplate.update(
                "UPDATE rag_document SET chunk_count=?, status=?, error_message=?, updated_at=? WHERE id=?",
                document.getChunkCount(), document.getStatus().name(),
                document.getErrorMessage(), document.getUpdatedAt(), document.getId()
        );
    }

    public RagDocument getDocumentById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM rag_document WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return mapToDocument(rows.getFirst());
    }

    public List<RagDocument> getDocumentsByKbConfigId(Long kbConfigId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM rag_document WHERE knowledge_base_config_id = ? ORDER BY created_at DESC", kbConfigId);
        List<RagDocument> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToDocument(row));
        }
        return result;
    }

    public void deleteDocument(Long id) {
        jdbcTemplate.update("DELETE FROM rag_document WHERE id = ?", id);
    }

    public void insertChunk(RagDocumentChunk chunk) {
        jdbcTemplate.update(
                "INSERT INTO rag_document_chunk (id, document_id, chunk_index, content, " +
                        "token_count, start_offset, end_offset, metadata, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                chunk.getId(), chunk.getDocumentId(), chunk.getChunkIndex(),
                chunk.getContent(), chunk.getTokenCount(), chunk.getStartOffset(),
                chunk.getEndOffset(), chunk.getMetadata(), chunk.getCreatedAt()
        );
    }

    public void batchInsertChunks(List<RagDocumentChunk> chunks) {
        for (RagDocumentChunk chunk : chunks) {
            insertChunk(chunk);
        }
    }

    public RagDocumentChunk getChunkById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM rag_document_chunk WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        return mapToChunk(rows.getFirst());
    }

    public List<RagDocumentChunk> getChunksByDocumentId(Long documentId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM rag_document_chunk WHERE document_id = ? ORDER BY chunk_index", documentId);
        List<RagDocumentChunk> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(mapToChunk(row));
        }
        return result;
    }

    public void deleteChunksByDocumentId(Long documentId) {
        jdbcTemplate.update("DELETE FROM rag_document_chunk WHERE document_id = ?", documentId);
    }

    public void updateChunk(RagDocumentChunk chunk) {
        jdbcTemplate.update(
                "UPDATE rag_document_chunk SET content=?, token_count=?, start_offset=?, end_offset=? WHERE id=?",
                chunk.getContent(), chunk.getTokenCount(), chunk.getStartOffset(),
                chunk.getEndOffset(), chunk.getId()
        );
    }

    public void deleteChunkById(Long chunkId) {
        jdbcTemplate.update("DELETE FROM rag_document_chunk WHERE id = ?", chunkId);
    }

    private RagDocument mapToDocument(Map<String, Object> row) {
        return RagDocument.builder()
                .id(((Number) row.get("id")).longValue())
                .knowledgeBaseConfigId(((Number) row.get("knowledge_base_config_id")).longValue())
                .fileName((String) row.get("file_name"))
                .filePath((String) row.get("file_path"))
                .fileSize(row.get("file_size") != null ? ((Number) row.get("file_size")).longValue() : 0L)
                .fileType((String) row.get("file_type"))
                .chunkCount(row.get("chunk_count") != null ? ((Number) row.get("chunk_count")).intValue() : 0)
                .status(RagDocumentStatus.valueOf((String) row.get("status")))
                .errorMessage((String) row.get("error_message"))
                .createdAt(toLocalDateTime(row.get("created_at")))
                .updatedAt(toLocalDateTime(row.get("updated_at")))
                .createdBy(row.get("created_by") != null ? ((Number) row.get("created_by")).longValue() : null)
                .updatedBy(row.get("updated_by") != null ? ((Number) row.get("updated_by")).longValue() : null)
                .build();
    }

    private RagDocumentChunk mapToChunk(Map<String, Object> row) {
        return RagDocumentChunk.builder()
                .id(((Number) row.get("id")).longValue())
                .documentId(((Number) row.get("document_id")).longValue())
                .chunkIndex(((Number) row.get("chunk_index")).intValue())
                .content((String) row.get("content"))
                .tokenCount(row.get("token_count") != null ? ((Number) row.get("token_count")).intValue() : null)
                .startOffset(row.get("start_offset") != null ? ((Number) row.get("start_offset")).intValue() : null)
                .endOffset(row.get("end_offset") != null ? ((Number) row.get("end_offset")).intValue() : null)
                .metadata((String) row.get("metadata"))
                .createdAt(toLocalDateTime(row.get("created_at")))
                .build();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
