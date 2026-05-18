package com.hxh.apboa.common.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpActivationStatus;
import com.hxh.apboa.common.enums.McpFailureSource;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 服务器 VO
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
    private McpActivationStatus activationStatus;
    private String activationMessage;
    private McpFailureSource failureSource;
    private LocalDateTime activationStatusChangedAt;
    private LocalDateTime lastActivationTime;
    private LocalDateTime lastToolSyncTime;
    private Integer toolCount;
    private Integer availableToolCount;
    private Integer runtimeFailThreshold;
    private Boolean needsSync;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
}
