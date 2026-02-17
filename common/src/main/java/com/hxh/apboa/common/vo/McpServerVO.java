package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MCP服务器VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class McpServerVO implements SerializableEnable {
    private Long id;
    private String name;
    private McpProtocol protocol;
    private McpMode mode;
    private Integer timeout;
    private JsonNode protocolConfig;
    private String description;
    private HealthStatus healthStatus;
    private LocalDateTime lastHealthCheck;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
