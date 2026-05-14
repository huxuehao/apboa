package com.hxh.apboa.common.mcp;

import com.hxh.apboa.common.entity.McpServer;

/**
 * 工具签名刷新器接口，由 core 模块实现以打破 mcp ↔ core 循环依赖。
 *
 * @author huxuehao
 */
public interface ToolSchemaRefresher {
    void refreshToolSchemas(McpServer server);
}
