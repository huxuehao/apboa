package com.hxh.apboa.common.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型配置VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class ModelConfigVO implements SerializableEnable {
    private Long id;
    private Long providerId;
    private String name;
    private String modelId;
    private JsonNode modelType;
    private String description;
    private Boolean streaming;
    private Boolean thinking;
    private Integer contextWindow;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Double repeatPenalty;
    private Long seed;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
