package com.hxh.apboa.mcp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.entity.McpTool;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.CryptoUtils;
import com.hxh.apboa.common.vo.McpToolVO;
import com.hxh.apboa.mcp.mapper.McpToolMapper;
import com.hxh.apboa.mcp.service.McpToolService;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * MCP 工具目录 Service 实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl extends ServiceImpl<McpToolMapper, McpTool> implements McpToolService {
    private static final String SCHEMA_HASH_SALT = "MCP_TOOL_SCHEMA_HASH";

    private final ObjectMapper objectMapper;

    @Override
    public List<McpToolVO> listToolVos(Long mcpServerId) {
        McpServer mcpServer = BeanUtils.getBean(com.hxh.apboa.mcp.service.McpServerService.class)
                .getById(mcpServerId);
        ensureBackfilledFromCache(mcpServer);
        return lambdaQuery()
                .eq(McpTool::getMcpServerId, mcpServerId)
                .orderByAsc(McpTool::getSort)
                .orderByAsc(McpTool::getToolName)
                .list()
                .stream()
                .map(item -> BeanUtils.copy(item, McpToolVO.class))
                .toList();
    }

    @Override
    public void ensureBackfilledFromCache(McpServer mcpServer) {
        if (mcpServer == null || mcpServer.getId() == null) {
            return;
        }
        long count = lambdaQuery().eq(McpTool::getMcpServerId, mcpServer.getId()).count();
        if (count > 0) {
            return;
        }

        List<McpSchema.Tool> cachedTools = parseCachedTools(mcpServer.getToolSchemas());
        if (cachedTools.isEmpty()) {
            return;
        }
        syncServerTools(mcpServer, cachedTools);
    }

    @Override
    public void syncServerTools(McpServer mcpServer, List<McpSchema.Tool> tools) {
        if (mcpServer == null || mcpServer.getId() == null) {
            return;
        }

        List<McpTool> existing = lambdaQuery()
                .eq(McpTool::getMcpServerId, mcpServer.getId())
                .list();
        Map<String, McpTool> existingMap = existing.stream().collect(Collectors.toMap(
                McpTool::getToolName,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new));

        LocalDateTime now = LocalDateTime.now();
        Set<String> currentNames = new LinkedHashSet<>();
        List<McpSchema.Tool> toolList = tools == null ? List.of() : tools;

        for (int i = 0; i < toolList.size(); i++) {
            McpSchema.Tool tool = toolList.get(i);
            if (tool == null || tool.name() == null || tool.name().isBlank()) {
                continue;
            }

            currentNames.add(tool.name());
            McpTool existingTool = existingMap.get(tool.name());
            McpTool entity = new McpTool();
            entity.setMcpServerId(mcpServer.getId());
            entity.setToolName(tool.name());
            entity.setDescription(tool.description());
            entity.setInputSchema(toJsonNode(tool.inputSchema()));
            entity.setOutputSchema(toJsonNode(tool.outputSchema()));
            entity.setRawSchema(toJsonNode(tool));
            entity.setSchemaHash(buildSchemaHash(tool));
            entity.setMissing(false);
            entity.setSort(i + 1);
            entity.setLastSeenAt(now);
            entity.setLastDiscoveredAt(existingTool == null ? now : existingTool.getLastDiscoveredAt());
            entity.setEnabled(existingTool == null || existingTool.getEnabled() == null
                    ? Boolean.TRUE
                    : existingTool.getEnabled());

            if (existingTool == null) {
                save(entity);
            } else {
                entity.setId(existingTool.getId());
                updateById(entity);
            }
        }

        List<McpTool> disappearedTools = existing.stream()
                .filter(item -> !currentNames.contains(item.getToolName()))
                .toList();
        disappearedTools.forEach(item -> {
            McpTool update = new McpTool();
            update.setId(item.getId());
            update.setMissing(true);
            updateById(update);
        });
    }

    @Override
    public void updateGlobalEnabled(Long mcpServerId, List<Long> toolIds, Boolean enabled) {
        if (toolIds == null || toolIds.isEmpty()) {
            return;
        }

        List<McpTool> tools = listByIdsPreserveOrder(toolIds);
        if (tools.size() != new LinkedHashSet<>(toolIds).size()) {
            throw new RuntimeException("存在无效的 MCP 工具选择");
        }
        boolean mismatch = tools.stream().anyMatch(item -> !Objects.equals(item.getMcpServerId(), mcpServerId));
        if (mismatch) {
            throw new RuntimeException("存在不属于当前 MCP 的工具");
        }

        lambdaUpdate()
                .in(McpTool::getId, toolIds)
                .set(McpTool::getEnabled, enabled)
                .update();
    }

    @Override
    public List<McpTool> listRuntimeTools(Long mcpServerId) {
        return lambdaQuery()
                .eq(McpTool::getMcpServerId, mcpServerId)
                .eq(McpTool::getEnabled, true)
                .eq(McpTool::getMissing, false)
                .orderByAsc(McpTool::getSort)
                .orderByAsc(McpTool::getToolName)
                .list();
    }

    @Override
    public List<McpTool> listByServerIds(List<Long> mcpServerIds) {
        if (mcpServerIds == null || mcpServerIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(McpTool::getMcpServerId, mcpServerIds)
                .list();
    }

    @Override
    public List<McpTool> listByIdsPreserveOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<McpTool> tools = listByIds(ids);
        Map<Long, McpTool> toolMap = tools.stream().collect(Collectors.toMap(McpTool::getId, Function.identity()));
        List<McpTool> ordered = new ArrayList<>();
        for (Long id : ids) {
            McpTool item = toolMap.get(id);
            if (item != null) {
                ordered.add(item);
            }
        }
        return ordered;
    }

    @Override
    public Map<Long, Integer> countAvailableTools(List<Long> mcpServerIds) {
        if (mcpServerIds == null || mcpServerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return lambdaQuery()
                .in(McpTool::getMcpServerId, mcpServerIds)
                .eq(McpTool::getEnabled, true)
                .eq(McpTool::getMissing, false)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        McpTool::getMcpServerId,
                        item -> 1,
                        Integer::sum));
    }

    @Override
    public void deleteByMcpServerIds(List<Long> mcpServerIds) {
        if (mcpServerIds == null || mcpServerIds.isEmpty()) {
            return;
        }
        lambdaUpdate().in(McpTool::getMcpServerId, mcpServerIds).remove();
    }

    private List<McpSchema.Tool> parseCachedTools(String toolSchemasJson) {
        if (toolSchemasJson == null || toolSchemasJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(toolSchemasJson, new TypeReference<List<McpSchema.Tool>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private JsonNode toJsonNode(Object value) {
        return value == null ? null : objectMapper.valueToTree(value);
    }

    private String buildSchemaHash(McpSchema.Tool tool) {
        return CryptoUtils.md5(toJsonNode(tool).toString(), SCHEMA_HASH_SALT);
    }
}
