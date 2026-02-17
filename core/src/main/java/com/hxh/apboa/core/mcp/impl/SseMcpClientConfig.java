package com.hxh.apboa.core.mcp.impl;

import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.key.McpSseKey;
import com.hxh.apboa.common.key.McpStdIOKey;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.mcp.McpClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;
import java.util.Map;

/**
 * 描述：Sse MCP客户端配置
 *
 * @author huxuehao
 **/
public class SseMcpClientConfig implements McpClientConfig {
    @Override
    public McpClientWrapper getMcpClient(McpServer mcpServer) {
        if (mcpServer.getProtocol() != protocol()) {
            throw new IllegalArgumentException("McpServer protocol must be SSE");
        }

        // 解析配置
        JsonNode config = JsonUtils.toJsonNode(mcpServer.getProtocolConfig());

        // 构建MCP客户端
        McpClientBuilder builder = McpClientBuilder.create(config.get(McpStdIOKey.name).asText())
                .sseTransport(config.get(McpSseKey.url).asText())
                .queryParams(getQueryParams(config))
                .headers(getHeaders(config))
                .timeout(Duration.ofSeconds(mcpServer.getTimeout()))
                .initializationTimeout(Duration.ofSeconds(30));

        // 构建MCP客户端并返回
        if (mcpServer.getMode() == McpMode.SYNC) {
            return builder.buildSync();
        } else {
            return builder.buildAsync().block();
        }
    }

    /**
     * 获取请求参数
     *
     * @param config 配置
     */
    private Map<String, String> getQueryParams(JsonNode config) {
        JsonNode jsonNode = config.get(McpSseKey.queryParams);
        return jsonNode2Map(jsonNode);
    }

    /**
     * 获取请求参数
     *
     * @param config 配置
     */
    private Map<String, String> getHeaders(JsonNode config) {
        JsonNode jsonNode = config.get(McpSseKey.headers);
        return jsonNode2Map(jsonNode);
    }

    @Override
    public McpProtocol protocol() {
        return McpProtocol.SSE;
    }
}
