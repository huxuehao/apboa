package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.AuthType;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.enums.ModelProviderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型提供商VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class ModelProviderVO implements SerializableEnable {
    private Long id;
    private ModelProviderType type;
    private String name;
    private String description;
    private String baseUrl;
    private AuthType authType;
    private String apiKey;
    private String envVarName;
    private Boolean enabled;
    private JsonNode configMeta;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
