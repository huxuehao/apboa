package com.hxh.apboa.mcp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.entity.McpTool;
import com.hxh.apboa.common.vo.McpToolVO;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具目录 Service
 *
 * @author huxuehao
 */
public interface McpToolService extends IService<McpTool> {
    List<McpToolVO> listToolVos(Long mcpServerId);

    void ensureBackfilledFromCache(McpServer mcpServer);

    void syncServerTools(McpServer mcpServer, List<McpSchema.Tool> tools);

    void updateGlobalEnabled(Long mcpServerId, List<Long> toolIds, Boolean enabled);

    List<McpTool> listRuntimeTools(Long mcpServerId);

    List<McpTool> listByServerIds(List<Long> mcpServerIds);

    List<McpTool> listByIdsPreserveOrder(List<Long> ids);

    Map<Long, Integer> countAvailableTools(List<Long> mcpServerIds);

    void deleteByMcpServerIds(List<Long> mcpServerIds);
}
