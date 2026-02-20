package com.hxh.apboa.core.mcp.impl;

import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.key.McpStdIOKey;
import com.hxh.apboa.core.mcp.McpClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

/**
 * 描述：Stdio MCP客户端配置
 *
 * @author huxuehao
 **/
public class StdioMcpClientConfig implements McpClientConfig {
    @Override
    public McpClientWrapper getMcpClient(McpServer mcpServer) {
        if (mcpServer.getProtocol() != protocol()) {
            throw new IllegalArgumentException("McpServer protocol must be STDIO");
        }

        // 解析配置
        JsonNode config = mcpServer.getProtocolConfig();

        // 构建MCP客户端
        McpClientBuilder builder = McpClientBuilder.create(mcpServer.getName())
                .stdioTransport(
                        getCommand(config),
                        getArgs(config),
                        jsonNode2Map(config.get(McpStdIOKey.env)))
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
     * 获取指令
     *
     * @param config 配置
     */
    private String getCommand(JsonNode config) {
        return config.get(McpStdIOKey.command).asText();
    }

    /**
     * 获取参数
     *
     * @param config 配置
     */
    private List<String> getArgs(JsonNode config) {
        List<String> args = new LinkedList<>();
        for (JsonNode jsonNode : config.get(McpStdIOKey.args)) {
            args.add(jsonNode.asText());
        }
        return args;
    }

    @Override
    public McpProtocol protocol() {
        return McpProtocol.STDIO;
    }
}
