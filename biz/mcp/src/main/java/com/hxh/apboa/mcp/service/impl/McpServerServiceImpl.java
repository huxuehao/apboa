package com.hxh.apboa.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.dto.McpToolEnabledDTO;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.AgentMcpServer;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.entity.McpTool;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpActivationStatus;
import com.hxh.apboa.common.enums.McpFailureSource;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.exception.BusinessException;
import com.hxh.apboa.common.mcp.ToolSchemaRefreshResult;
import com.hxh.apboa.common.mcp.ToolSchemaRefresher;
import com.hxh.apboa.common.util.CryptoUtils;
import com.hxh.apboa.common.vo.McpToolVO;
import com.hxh.apboa.mcp.mapper.McpServerMapper;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.AgentMcpToolService;
import com.hxh.apboa.mcp.service.McpRuntimeDegradeService;
import com.hxh.apboa.mcp.service.McpServerService;
import com.hxh.apboa.mcp.service.McpToolService;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * MCP 服务配置 Service 实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class McpServerServiceImpl extends ServiceImpl<McpServerMapper, McpServer> implements McpServerService {
    private static final String CONFIG_HASH_SALT = "MCP_CONFIG_HASH";
    private static final int DEFAULT_RUNTIME_FAIL_THRESHOLD = 3;

    private final JdbcTemplate jdbcTemplate;
    private final AgentMcpServerService agentMcpServerService;
    private final AgentMcpToolService agentMcpToolService;
    private final McpToolService mcpToolService;
    private final MessagePublisher messagePublisher;
    private final ToolSchemaRefresher toolSchemaRefresher;
    private final McpRuntimeDegradeService mcpRuntimeDegradeService;
    private final ObjectMapper objectMapper;

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        getAgentDefinitions(agentMcpServerService.getAgentIds(ids)).forEach(agentDefinition ->
                names.add(agentDefinition.getName()));
        return names;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        List<Long> agentIds = agentMcpServerService.getAgentIds(ids);
        List<Long> mcpToolIds = mcpToolService.listByServerIds(ids)
                .stream()
                .map(McpTool::getId)
                .toList();
        agentMcpToolService.deleteByMcpToolIds(mcpToolIds);
        mcpToolService.deleteByMcpServerIds(ids);
        removeByIds(ids);
        boolean result = agentMcpServerService.remove(
                new LambdaQueryWrapper<AgentMcpServer>().in(AgentMcpServer::getMcpServerId, ids));
        publishAgentReregisterAfterCommit(agentIds);
        return result;
    }

    @Override
    public boolean save(McpServer entity) {
        LocalDateTime now = LocalDateTime.now();
        entity.setActivationStatus(McpActivationStatus.NOT_ACTIVATED);
        entity.setActivationMessage("未连接");
        entity.setFailureSource(McpFailureSource.NONE);
        entity.setActivationStatusChangedAt(now);
        entity.setToolCount(0);
        entity.setActivationRevision(0L);
        entity.setNeedsSync(true);
        entity.setRuntimeFailThreshold(normalizeRuntimeFailThreshold(entity.getRuntimeFailThreshold()));
        entity.setConfigHash(buildConfigHash(
                entity.getProtocol(),
                entity.getMode(),
                entity.getTimeout(),
                entity.getProtocolConfig()));
        return super.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer doUpdate(McpServer entity) {
        McpServer current = requireServer(entity.getId());
        String oldConfigHash = current.getConfigHash();
        if (oldConfigHash == null || oldConfigHash.isBlank()) {
            oldConfigHash = buildConfigHash(
                    current.getProtocol(),
                    current.getMode(),
                    current.getTimeout(),
                    current.getProtocolConfig());
        }

        if (entity.getRuntimeFailThreshold() != null) {
            entity.setRuntimeFailThreshold(normalizeRuntimeFailThreshold(entity.getRuntimeFailThreshold()));
        }

        String mergedConfigHash = buildConfigHash(
                firstNonNull(entity.getProtocol(), current.getProtocol()),
                firstNonNull(entity.getMode(), current.getMode()),
                firstNonNull(entity.getTimeout(), current.getTimeout()),
                firstNonNull(entity.getProtocolConfig(), current.getProtocolConfig()));
        entity.setConfigHash(mergedConfigHash);

        updateById(entity);

        McpServer updated = requireServer(entity.getId());
        boolean configChanged = !Objects.equals(oldConfigHash, mergedConfigHash);
        boolean shouldAutoActivate = current.getActivationStatus() == McpActivationStatus.ACTIVE
                && configChanged
                && Boolean.TRUE.equals(updated.getEnabled());

        if (shouldAutoActivate) {
            return activate(updated.getId());
        }

        publishAgentReregisterAfterCommit(agentMcpServerService.getAgentIds(List.of(updated.getId())));
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer activate(Long id) {
        return refreshServer(id, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer syncTools(Long id) {
        return refreshServer(id, false);
    }

    @Override
    public List<McpToolVO> listTools(Long id) {
        McpServer server = requireServer(id);
        mcpToolService.ensureBackfilledFromCache(server);
        return mcpToolService.listToolVos(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public McpServer updateToolGlobalEnabled(Long id, McpToolEnabledDTO dto) {
        McpServer server = requireServer(id);
        ensureToolGovernanceWritable(server);
        List<Long> toolIds = dto == null || dto.getToolIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(dto.getToolIds()));
        if (!toolIds.isEmpty()) {
            mcpToolService.updateGlobalEnabled(id, toolIds, Boolean.TRUE.equals(dto.getEnabled()));
            publishAgentReregisterAfterCommit(agentMcpServerService.getAgentIds(List.of(id)));
        }
        return requireServer(id);
    }

    private McpServer refreshServer(Long id, boolean markActivationTime) {
        McpServer current = requireServer(id);
        String configHash = buildConfigHash(
                current.getProtocol(),
                current.getMode(),
                current.getTimeout(),
                current.getProtocolConfig());
        String requestId = CryptoUtils.uuid();

        if (!beginActivation(current, requestId, configHash)) {
            return requireServer(id);
        }

        current = requireServer(id);
        ToolSchemaRefreshResult refreshResult = toolSchemaRefresher.refreshToolSchemas(current);
        LocalDateTime now = LocalDateTime.now();
        McpServer update = new McpServer();
        update.setId(id);
        update.setActivationMessage(refreshResult.getMessage());
        update.setFailureSource(McpFailureSource.NONE);
        update.setActivationStatusChangedAt(now);
        update.setLastToolSyncTime(now);
        update.setLastHealthCheck(now);
        update.setConfigHash(configHash);
        update.setNeedsSync(!refreshResult.isSuccess());

        if (markActivationTime) {
            update.setLastActivationTime(now);
        }

        if (refreshResult.isSuccess()) {
            update.setActivationStatus(McpActivationStatus.ACTIVE);
            update.setToolSchemas(refreshResult.getToolSchemas());
            update.setToolCount(refreshResult.getToolCount());
            update.setHealthStatus(HealthStatus.HEALTHY);
            update.setActivationRevision(nextRevision(current.getActivationRevision()));
        } else {
            update.setActivationStatus(McpActivationStatus.FAILED);
            update.setHealthStatus(HealthStatus.UNHEALTHY);
        }

        boolean applied = finishActivation(id, requestId, configHash, update);
        if (applied && refreshResult.isSuccess()) {
            McpServer refreshed = requireServer(id);
            mcpToolService.syncServerTools(refreshed, parseToolSchemas(refreshResult.getToolSchemas()));
            mcpRuntimeDegradeService.recordSuccess(
                    refreshed.getId(),
                    refreshed.getActivationRevision(),
                    refreshed.getConfigHash(),
                    refreshed.getRuntimeFailThreshold());
        }
        if (applied) {
            publishAgentReregisterAfterCommit(agentMcpServerService.getAgentIds(List.of(id)));
        }
        return requireServer(id);
    }

    private boolean beginActivation(McpServer current, String requestId, String configHash) {
        McpServer update = new McpServer();
        update.setActivationStatus(McpActivationStatus.ACTIVATING);
        update.setActivationMessage("正在连接 MCP 并刷新工具目录");
        update.setFailureSource(McpFailureSource.NONE);
        update.setActivationStatusChangedAt(LocalDateTime.now());
        update.setActivationRequestId(requestId);
        update.setConfigHash(configHash);
        update.setNeedsSync(true);

        LambdaUpdateWrapper<McpServer> wrapper = new LambdaUpdateWrapper<McpServer>()
                .eq(McpServer::getId, current.getId());
        if (current.getActivationRequestId() == null) {
            wrapper.isNull(McpServer::getActivationRequestId);
        } else {
            wrapper.eq(McpServer::getActivationRequestId, current.getActivationRequestId());
        }

        return baseMapper.update(update, wrapper) > 0;
    }

    private boolean finishActivation(Long id, String requestId, String configHash, McpServer update) {
        LambdaUpdateWrapper<McpServer> wrapper = new LambdaUpdateWrapper<McpServer>()
                .eq(McpServer::getId, id)
                .eq(McpServer::getActivationRequestId, requestId)
                .eq(McpServer::getConfigHash, configHash);
        return baseMapper.update(update, wrapper) > 0;
    }

    private McpServer requireServer(Long id) {
        McpServer server = getById(id);
        if (server == null) {
            throw new RuntimeException("MCP 服务不存在");
        }
        return server;
    }

    private List<McpSchema.Tool> parseToolSchemas(String toolSchemas) {
        if (toolSchemas == null || toolSchemas.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(toolSchemas, new TypeReference<List<McpSchema.Tool>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("MCP 工具目录解析失败: " + e.getMessage(), e);
        }
    }

    private long nextRevision(Long currentRevision) {
        return currentRevision == null ? 1L : currentRevision + 1L;
    }

    private int normalizeRuntimeFailThreshold(Integer runtimeFailThreshold) {
        if (runtimeFailThreshold == null) {
            return DEFAULT_RUNTIME_FAIL_THRESHOLD;
        }
        return Math.max(runtimeFailThreshold, 0);
    }

    private String buildConfigHash(McpProtocol protocol,
                                   McpMode mode,
                                   Integer timeout,
                                   JsonNode protocolConfig) {
        String configJson = protocolConfig == null ? "" : protocolConfig.toString();
        String raw = (protocol == null ? "" : protocol.name())
                + "|"
                + (mode == null ? "" : mode.name())
                + "|"
                + (timeout == null ? "" : timeout)
                + "|"
                + configJson;
        return CryptoUtils.md5(raw, CONFIG_HASH_SALT);
    }

    private void ensureToolGovernanceWritable(McpServer server) {
        if (server.getActivationStatus() == McpActivationStatus.FAILED
                && server.getFailureSource() == McpFailureSource.RUNTIME_AUTO_DEGRADE) {
            throw new BusinessException("当前 MCP 处于运行时自动降级状态，仅可查看上次缓存，重新连接成功前不可修改工具治理");
        }
    }

    private <T> T firstNonNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private void publishAgentReregisterAfterCommit(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return;
        }

        Runnable publishAction = () -> agentIds.forEach(agentId ->
                messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(agentId)));

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }

        publishAction.run();
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = agentIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String sql = String.format("SELECT * FROM %s WHERE id IN (%s)", TableConst.AGENT, subSql);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AgentDefinition agent = new AgentDefinition();
            agent.setId(rs.getLong("id"));
            agent.setName(rs.getString("name"));
            agent.setDescription(rs.getString("description"));
            return agent;
        });
    }
}
