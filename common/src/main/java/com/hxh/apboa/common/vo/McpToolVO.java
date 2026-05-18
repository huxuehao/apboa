package com.hxh.apboa.common.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.SerializableEnable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * MCP 工具视图对象
 *
 * @author huxuehao
 */
@Data
public class McpToolVO implements SerializableEnable {
    private Long id;
    private Long mcpServerId;
    private String toolName;
    private String description;
    private JsonNode inputSchema;
    private JsonNode outputSchema;
    private Boolean enabled;
    private Boolean missing;
    private Integer sort;
    private LocalDateTime lastDiscoveredAt;
    private LocalDateTime lastSeenAt;
}
