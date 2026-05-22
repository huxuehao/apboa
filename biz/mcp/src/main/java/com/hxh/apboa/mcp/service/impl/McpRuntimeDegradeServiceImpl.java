package com.hxh.apboa.mcp.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpActivationStatus;
import com.hxh.apboa.common.enums.McpFailureSource;
import com.hxh.apboa.mcp.mapper.McpServerMapper;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.McpRuntimeDegradeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * MCP 运行时自动降级服务实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class McpRuntimeDegradeServiceImpl implements McpRuntimeDegradeService {
    private static final Logger log = LoggerFactory.getLogger(McpRuntimeDegradeServiceImpl.class);
    private static final String KEY_PREFIX = "apboa:mcp:runtime:degrade:";
    private static final long KEY_TTL_SECONDS = 3600L;
    private static final String FAILURE_EVENT = "FAILURE";

    private static final DefaultRedisScript<String> RECORD_FAILURE_SCRIPT = buildScript("""
            local key = KEYS[1]
            local threshold = tonumber(ARGV[1])
            local ttlSeconds = tonumber(ARGV[2])
            local count = tonumber(redis.call('HGET', key, 'failure_count') or '0')
            local seq = tonumber(redis.call('HGET', key, 'event_seq') or '0')
            seq = seq + 1
            count = count + 1
            redis.call('HSET', key,
                'failure_count', tostring(count),
                'event_seq', tostring(seq),
                'last_event_type', 'FAILURE')
            redis.call('EXPIRE', key, ttlSeconds)
            local reached = 0
            if threshold > 0 and count >= threshold then
                reached = 1
            end
            return tostring(count) .. '|' .. tostring(seq) .. '|' .. tostring(reached) .. '|FAILURE'
            """);

    private static final DefaultRedisScript<String> RECORD_SUCCESS_SCRIPT = buildScript("""
            local key = KEYS[1]
            local ttlSeconds = tonumber(ARGV[1])
            local seq = tonumber(redis.call('HGET', key, 'event_seq') or '0')
            seq = seq + 1
            redis.call('HSET', key,
                'failure_count', '0',
                'event_seq', tostring(seq),
                'last_event_type', 'SUCCESS')
            redis.call('EXPIRE', key, ttlSeconds)
            return '0|' .. tostring(seq) .. '|0|SUCCESS'
            """);

    private final StringRedisTemplate stringRedisTemplate;
    private final McpRuntimeFailureClassifier failureClassifier;
    private final McpServerMapper mcpServerMapper;
    private final AgentMcpServerService agentMcpServerService;
    private final MessagePublisher messagePublisher;

    @Override
    public void recordSuccess(Long serverId,
                              Long activationRevision,
                              String configHash,
                              Integer runtimeFailThreshold) {
        if (!shouldTrack(serverId, activationRevision, configHash, runtimeFailThreshold)) {
            return;
        }

        try {
            RuntimeDegradeState state = executeStateScript(
                    RECORD_SUCCESS_SCRIPT,
                    buildKey(serverId, activationRevision, configHash),
                    String.valueOf(KEY_TTL_SECONDS));
            log.info("MCP 运行时成功已清零连续失败计数: serverId={}, activationRevision={}, configHash={}, eventSeq={}",
                    serverId, activationRevision, configHash, state.eventSeq());
        } catch (Exception e) {
            log.warn("记录 MCP 运行时成功失败，已忽略自动降级状态更新: serverId={}, activationRevision={}, configHash={}, reason={}",
                    serverId, activationRevision, configHash, e.getMessage(), e);
        }
    }

    @Override
    public void recordFailure(Long serverId,
                              Long activationRevision,
                              String configHash,
                              Integer runtimeFailThreshold,
                              Throwable throwable) {
        if (!shouldTrack(serverId, activationRevision, configHash, runtimeFailThreshold)) {
            return;
        }
        if (!failureClassifier.isTransportFailure(throwable)) {
            return;
        }

        try {
            RuntimeDegradeState state = executeStateScript(
                    RECORD_FAILURE_SCRIPT,
                    buildKey(serverId, activationRevision, configHash),
                    String.valueOf(runtimeFailThreshold),
                    String.valueOf(KEY_TTL_SECONDS));
            log.warn("MCP 运行时失败已计数: serverId={}, activationRevision={}, configHash={}, failureCount={}, eventSeq={}",
                    serverId, activationRevision, configHash, state.failureCount(), state.eventSeq());
            if (state.reachedThreshold()) {
                tryAutoDegrade(serverId, activationRevision, configHash, state.eventSeq(), throwable);
            }
        } catch (Exception e) {
            log.warn("记录 MCP 运行时失败失败，已忽略自动降级状态更新: serverId={}, activationRevision={}, configHash={}, reason={}",
                    serverId, activationRevision, configHash, e.getMessage(), e);
        }
    }

    private void tryAutoDegrade(Long serverId,
                                Long activationRevision,
                                String configHash,
                                long observedEventSeq,
                                Throwable throwable) {
        RuntimeDegradeState currentState = getCurrentState(serverId, activationRevision, configHash);
        if (currentState == null
                || currentState.eventSeq() != observedEventSeq
                || !FAILURE_EVENT.equals(currentState.lastEventType())) {
            return;
        }

        McpServer current = mcpServerMapper.selectById(serverId);
        if (current == null
                || !Boolean.TRUE.equals(current.getEnabled())
                || current.getActivationStatus() != McpActivationStatus.ACTIVE
                || !equalsLong(current.getActivationRevision(), activationRevision)
                || !equalsString(current.getConfigHash(), configHash)
                || normalizeThreshold(current.getRuntimeFailThreshold()) <= 0
                || currentState.failureCount() < normalizeThreshold(current.getRuntimeFailThreshold())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        McpServer update = new McpServer();
        update.setActivationStatus(McpActivationStatus.FAILED);
        update.setActivationMessage(buildFailureMessage(
                currentState.failureCount(),
                current.getRuntimeFailThreshold(),
                throwable));
        update.setFailureSource(McpFailureSource.RUNTIME_AUTO_DEGRADE);
        update.setActivationStatusChangedAt(now);
        update.setHealthStatus(HealthStatus.UNHEALTHY);
        update.setLastHealthCheck(now);
        update.setNeedsSync(true);

        LambdaUpdateWrapper<McpServer> wrapper = new LambdaUpdateWrapper<McpServer>()
                .eq(McpServer::getId, serverId)
                .eq(McpServer::getEnabled, true)
                .eq(McpServer::getActivationStatus, McpActivationStatus.ACTIVE)
                .eq(McpServer::getActivationRevision, activationRevision)
                .eq(McpServer::getConfigHash, configHash);
        int updated = mcpServerMapper.update(update, wrapper);
        if (updated <= 0) {
            return;
        }

        log.warn("MCP 已触发运行时自动降级: serverId={}, activationRevision={}, configHash={}, failureCount={}, eventSeq={}",
                serverId, activationRevision, configHash, currentState.failureCount(), currentState.eventSeq());
        publishAgentReregister(agentMcpServerService.getAgentIds(List.of(serverId)));
    }

    private RuntimeDegradeState getCurrentState(Long serverId, Long activationRevision, String configHash) {
        try {
            Map<Object, Object> stateMap = stringRedisTemplate.opsForHash().entries(buildKey(serverId, activationRevision, configHash));
            if (stateMap == null || stateMap.isEmpty()) {
                return null;
            }
            return new RuntimeDegradeState(
                    toLong(stateMap.get("failure_count")),
                    toLong(stateMap.get("event_seq")),
                    false,
                    String.valueOf(stateMap.getOrDefault("last_event_type", "")));
        } catch (Exception e) {
            log.warn("读取 MCP 自动降级状态失败，已忽略本次自动降级判定: serverId={}, activationRevision={}, configHash={}, reason={}",
                    serverId, activationRevision, configHash, e.getMessage(), e);
            return null;
        }
    }

    private RuntimeDegradeState executeStateScript(DefaultRedisScript<String> script, String key, String... args) {
        String result = stringRedisTemplate.execute(script, List.of(key), (Object[]) args);
        if (!StringUtils.hasText(result)) {
            throw new IllegalStateException("Redis 脚本未返回状态");
        }
        return parseState(result);
    }

    private RuntimeDegradeState parseState(String result) {
        String[] parts = result.split("\\|", 4);
        if (parts.length != 4) {
            throw new IllegalStateException("Redis 脚本返回格式不正确: " + result);
        }
        return new RuntimeDegradeState(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1]),
                "1".equals(parts[2]),
                parts[3]);
    }

    private void publishAgentReregister(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return;
        }
        agentIds.forEach(agentId ->
                messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(agentId)));
    }

    private String buildKey(Long serverId, Long activationRevision, String configHash) {
        return KEY_PREFIX + serverId + ":" + activationRevision + ":" + configHash;
    }

    private boolean shouldTrack(Long serverId,
                                Long activationRevision,
                                String configHash,
                                Integer runtimeFailThreshold) {
        return serverId != null
                && activationRevision != null
                && StringUtils.hasText(configHash)
                && normalizeThreshold(runtimeFailThreshold) > 0;
    }

    private int normalizeThreshold(Integer threshold) {
        if (threshold == null) {
            return 3;
        }
        return Math.max(threshold, 0);
    }

    private boolean equalsLong(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean equalsString(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String buildFailureMessage(long failureCount, Integer threshold, Throwable throwable) {
        String reason = throwable == null || !StringUtils.hasText(throwable.getMessage())
                ? "连接或传输异常"
                : throwable.getMessage();
        return "运行时自动降级：连续 " + failureCount + " 次连接或传输失败，已达到阈值 "
                + normalizeThreshold(threshold) + "。请检查服务后手动连接或刷新工具。原因：" + reason;
    }

    private static DefaultRedisScript<String> buildScript(String scriptText) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(scriptText);
        script.setResultType(String.class);
        return script;
    }

    private record RuntimeDegradeState(long failureCount,
                                       long eventSeq,
                                       boolean reachedThreshold,
                                       String lastEventType) {
    }
}
