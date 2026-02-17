package com.hxh.apboa.core.mcp;

import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：MCP客户端配置接口
 *
 * @author huxuehao
 **/
public interface McpClientConfig {
    McpClientWrapper getMcpClient(McpServer mcpServer);

    McpProtocol protocol();

    /**
     * JsonNode 转 Map
     */
    default Map<String, String> jsonNode2Map(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isEmpty()) {
            return Map.of();
        }

        HashMap<String, String> map = new HashMap<>();
        for (JsonNode node : jsonNode) {
            if (node.has("key") && node.has("value")) {
                map.put(node.get("key").asText(), node.get("value").asText());
            }
        }

        return map;
    }
}
