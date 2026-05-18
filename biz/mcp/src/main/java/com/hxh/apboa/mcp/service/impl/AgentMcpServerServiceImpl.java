package com.hxh.apboa.mcp.service.impl;

import com.hxh.apboa.common.entity.AgentMcpServer;
import com.hxh.apboa.common.entity.McpTool;
import com.hxh.apboa.common.enums.McpToolExposureMode;
import com.hxh.apboa.common.vo.AgentMcpBindingVO;
import com.hxh.apboa.mcp.mapper.AgentMcpServerMapper;
import com.hxh.apboa.mcp.service.AgentMcpToolService;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.McpToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * 智能体MCP服务器关联Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class AgentMcpServerServiceImpl extends ServiceImpl<AgentMcpServerMapper, AgentMcpServer>
        implements AgentMcpServerService {
    private final AgentMcpToolService agentMcpToolService;
    private final McpToolService mcpToolService;

    @Override
    public List<Long> getAgentIds(List<Long> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return List.of();
        }
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
        return listByAgentDefinitionId(agentDefinitionId)
                .stream()
                .map(AgentMcpServer::getMcpServerId)
                .toList();
    }

    @Override
    public List<AgentMcpServer> listByAgentDefinitionId(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentMcpServer::getAgentDefinitionId, agentDefinitionId)
                .list();
    }

    @Override
    public List<AgentMcpBindingVO> getBindings(Long agentDefinitionId) {
        List<AgentMcpServer> bindings = listByAgentDefinitionId(agentDefinitionId);
        if (bindings.isEmpty()) {
            return List.of();
        }

        List<Long> selectedToolIds = agentMcpToolService.getToolIds(agentDefinitionId);
        Map<Long, List<Long>> toolIdsByServerId = new LinkedHashMap<>();
        if (!selectedToolIds.isEmpty()) {
            List<McpTool> tools = mcpToolService.listByIdsPreserveOrder(selectedToolIds);
            tools.forEach(tool -> toolIdsByServerId
                    .computeIfAbsent(tool.getMcpServerId(), key -> new ArrayList<>())
                    .add(tool.getId()));
        }

        return bindings.stream().map(binding -> {
            AgentMcpBindingVO vo = new AgentMcpBindingVO();
            vo.setMcpServerId(binding.getMcpServerId());
            vo.setExposureMode(binding.getExposureMode() == null
                    ? McpToolExposureMode.ALL_GLOBAL
                    : binding.getExposureMode());
            vo.setMcpToolIds(toolIdsByServerId.getOrDefault(binding.getMcpServerId(), List.of()));
            return vo;
        }).toList();
    }

    @Override
    public Boolean insertAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds) {
        if (mcpIds == null || mcpIds.isEmpty()) {
            return Boolean.TRUE;
        }
        mcpIds.forEach(mcpId -> save(new AgentMcpServer(
                null,
                agentDefinitionId,
                mcpId,
                McpToolExposureMode.ALL_GLOBAL)));

        return true;
    }

    @Override
    public Boolean deleteAgentMcpServer(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        agentMcpToolService.deleteAgentMcpToolByAgentIds(agentIds);
        return lambdaUpdate().in(AgentMcpServer::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentMcpServer(Long agentDefinitionId, List<Long> mcpIds, List<AgentMcpBindingVO> bindings) {
        List<AgentMcpBindingVO> normalizedBindings = normalizeBindings(mcpIds, bindings);
        List<Long> selectedToolIds = collectAndValidateToolIds(normalizedBindings);

        deleteAgentMcpServer(List.of(agentDefinitionId));
        normalizedBindings.forEach(binding -> save(new AgentMcpServer(
                null,
                agentDefinitionId,
                binding.getMcpServerId(),
                binding.getExposureMode() == null ? McpToolExposureMode.ALL_GLOBAL : binding.getExposureMode())));
        agentMcpToolService.replaceAgentMcpTools(agentDefinitionId, selectedToolIds);
        return Boolean.TRUE;
    }

    private List<AgentMcpBindingVO> normalizeBindings(List<Long> mcpIds, List<AgentMcpBindingVO> bindings) {
        if (bindings != null) {
            Map<Long, AgentMcpBindingVO> bindingMap = new LinkedHashMap<>();
            bindings.stream()
                    .filter(item -> item != null && item.getMcpServerId() != null)
                    .forEach(item -> {
                        AgentMcpBindingVO normalized = new AgentMcpBindingVO();
                        normalized.setMcpServerId(item.getMcpServerId());
                        normalized.setExposureMode(item.getExposureMode() == null
                                ? McpToolExposureMode.ALL_GLOBAL
                                : item.getExposureMode());
                        normalized.setMcpToolIds(item.getMcpToolIds() == null
                                ? List.of()
                                : new ArrayList<>(new LinkedHashSet<>(item.getMcpToolIds())));
                        bindingMap.put(item.getMcpServerId(), normalized);
                    });
            return new ArrayList<>(bindingMap.values());
        }

        if (mcpIds == null || mcpIds.isEmpty()) {
            return List.of();
        }

        return new LinkedHashSet<>(mcpIds).stream().map(mcpId -> {
            AgentMcpBindingVO binding = new AgentMcpBindingVO();
            binding.setMcpServerId(mcpId);
            binding.setExposureMode(McpToolExposureMode.ALL_GLOBAL);
            binding.setMcpToolIds(List.of());
            return binding;
        }).toList();
    }

    private List<Long> collectAndValidateToolIds(List<AgentMcpBindingVO> bindings) {
        List<Long> selectedToolIds = bindings.stream()
                .flatMap(item -> item.getMcpToolIds() == null ? List.<Long>of().stream() : item.getMcpToolIds().stream())
                .distinct()
                .toList();
        if (selectedToolIds.isEmpty()) {
            return List.of();
        }

        List<McpTool> tools = mcpToolService.listByIdsPreserveOrder(selectedToolIds);
        if (tools.size() != selectedToolIds.size()) {
            throw new RuntimeException("存在无效的 MCP 工具选择");
        }
        Map<Long, Long> toolServerMap = tools.stream().collect(LinkedHashMap::new,
                (map, item) -> map.put(item.getId(), item.getMcpServerId()),
                Map::putAll);
        boolean mismatch = bindings.stream().anyMatch(binding -> {
            List<Long> toolIds = binding.getMcpToolIds();
            if (toolIds == null || toolIds.isEmpty()) {
                return false;
            }
            return toolIds.stream().anyMatch(toolId ->
                    !Objects.equals(toolServerMap.get(toolId), binding.getMcpServerId()));
        });
        if (mismatch) {
            throw new RuntimeException("存在不属于当前 MCP 的工具选择");
        }
        return selectedToolIds;
    }
}
