package com.hxh.apboa.mcp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.AgentMcpTool;
import java.util.List;

/**
 * Agent MCP 工具关联 Service
 *
 * @author huxuehao
 */
public interface AgentMcpToolService extends IService<AgentMcpTool> {
    List<Long> getToolIds(Long agentDefinitionId);

    Boolean replaceAgentMcpTools(Long agentDefinitionId, List<Long> mcpToolIds);

    Boolean deleteAgentMcpToolByAgentIds(List<Long> agentIds);

    Boolean deleteByMcpToolIds(List<Long> mcpToolIds);
}
