package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.CodeLanguage;
import com.hxh.apboa.common.enums.ToolType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class ToolVO implements SerializableEnable {
    private Long id;
    private String name;
    private String toolId;
    private String description;
    private CodeLanguage language;
    private String category;
    private ToolType toolType;
    private JsonNode inputSchema;
    private JsonNode outputSchema;
    private Boolean needConfirm;
    private String classPath;
    private String code;
    private String version;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
