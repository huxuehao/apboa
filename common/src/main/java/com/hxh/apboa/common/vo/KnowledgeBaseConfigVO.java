package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.KbType;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.rag.RAGMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库配置VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class KnowledgeBaseConfigVO implements SerializableEnable {
    private Long id;
    private String name;
    private KbType kbType;
    private RAGMode ragMode;
    private String description;
    private JsonNode connectionConfig;
    private JsonNode endpointConfig;
    private JsonNode retrievalConfig;
    private JsonNode rerankingConfig;
    private JsonNode queryRewriteConfig;
    private JsonNode metadataFilters;
    private JsonNode httpConfig;
    private HealthStatus healthStatus;
    private LocalDateTime lastSyncTime;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
