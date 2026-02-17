package com.hxh.apboa.mcp.service.impl;

import com.hxh.apboa.common.entity.AgentMcpServer;
import com.hxh.apboa.mcp.mapper.AgentMcpServerMapper;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体MCP服务器关联Service实现
 *
 * @author huxuehao
 */
@Service
public class AgentMcpServerServiceImpl extends ServiceImpl<AgentMcpServerMapper, AgentMcpServer> implements AgentMcpServerService {
    @Override
    public List<Long> getAgentIds(List<Long> mcpIds) {
        return lambdaQuery()
                .in(AgentMcpServer::getMcpServerId, mcpIds)
                .list()
                .stream()
                .map(AgentMcpServer::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getMcpIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentMcpServer::getAgentDefinitionId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentMcpServer::getMcpServerId)
                .toList();
    }

    @Override
    public Boolean insertAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds) {
        mcpIds.forEach(mcpId -> {
            save(new AgentMcpServer(null, agentDefinitionId, mcpId));
        });

        return true;
    }

    @Override
    public Boolean deleteAgentMcpServer(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentMcpServer::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds) {
        deleteAgentMcpServer(List.of(agentDefinitionId));
        insertAgentMcpServer(agentDefinitionId, mcpIds);

        return Boolean.TRUE;
    }
}
