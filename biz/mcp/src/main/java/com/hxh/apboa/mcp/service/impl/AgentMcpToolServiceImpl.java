package com.hxh.apboa.mcp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.AgentMcpTool;
import com.hxh.apboa.mcp.mapper.AgentMcpToolMapper;
import com.hxh.apboa.mcp.service.AgentMcpToolService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Agent MCP 工具关联 Service 实现
 *
 * @author huxuehao
 */
@Service
public class AgentMcpToolServiceImpl extends ServiceImpl<AgentMcpToolMapper, AgentMcpTool>
        implements AgentMcpToolService {

    @Override
    public List<Long> getToolIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentMcpTool::getAgentDefinitionId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentMcpTool::getMcpToolId)
                .toList();
    }

    @Override
    public Boolean replaceAgentMcpTools(Long agentDefinitionId, List<Long> mcpToolIds) {
        deleteAgentMcpToolByAgentIds(List.of(agentDefinitionId));
        if (mcpToolIds == null || mcpToolIds.isEmpty()) {
            return Boolean.TRUE;
        }

        Set<Long> distinctIds = new LinkedHashSet<>(mcpToolIds);
        distinctIds.forEach(toolId -> save(new AgentMcpTool(null, agentDefinitionId, toolId)));
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteAgentMcpToolByAgentIds(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return Boolean.TRUE;
        }
        return lambdaUpdate().in(AgentMcpTool::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean deleteByMcpToolIds(List<Long> mcpToolIds) {
        if (mcpToolIds == null || mcpToolIds.isEmpty()) {
            return Boolean.TRUE;
        }
        return lambdaUpdate().in(AgentMcpTool::getMcpToolId, mcpToolIds).remove();
    }
}
