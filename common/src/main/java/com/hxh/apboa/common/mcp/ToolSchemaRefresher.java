package com.hxh.apboa.common.mcp;

import com.hxh.apboa.common.entity.McpServer;

/**
 * 工具目录刷新器接口，由 core 模块实现以打破 mcp 与 core 的循环依赖。
 *
 * @author huxuehao
 */
public interface ToolSchemaRefresher {
    ToolSchemaRefreshResult refreshToolSchemas(McpServer server);
}
