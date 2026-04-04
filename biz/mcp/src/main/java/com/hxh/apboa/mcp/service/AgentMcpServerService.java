package com.hxh.apboa.mcp.service;

import com.hxh.apboa.common.entity.AgentMcpServer;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 智能体MCP服务器关联Service
 *
 * @author huxuehao
 */
public interface AgentMcpServerService extends IService<AgentMcpServer> {
    List<Long> getAgentIds(List<Long> mcpIds);
    List<Long> getMcpIds(Long agentDefinitionId);
    Boolean insertAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds);
    Boolean deleteAgentMcpServer(List<Long> agentIds);
    Boolean saveAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds);
}
