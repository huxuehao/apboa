package com.hxh.apboa.core.agui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.AccountVO;
import io.agentscope.core.agui.model.RunAgentInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 描述：智能体上下文
 *
 * @author huxuehao
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {
    private static final ThreadLocal<AgentContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private String threadId;
    private String runId;
    private boolean memoryActive;
    private boolean planActive;
    private List<String> fileIds;
    private AccountVO userInfo;
    private AgentDefinition agentDefinition;
    private Map<String, Object> params;

    public static void init(RunAgentInput input, String threadId) {
        // 每次请求创建全新的上下文，避免复用旧上下文导致租户信息串扰
        AgentContext agentContext = new AgentContext();

        agentContext.setThreadId(threadId);

        agentContext.setRunId(input.getRunId());

        boolean memoryActive = input.getForwardedProp("memoryActive") != null
                ? (Boolean) input.getForwardedProp("memoryActive")
                : false;
        agentContext.setMemoryActive(memoryActive);

        agentContext.setPlanActive(
                input.getForwardedProp("planActive") != null
                        ? (Boolean) input.getForwardedProp("planActive")
                        : false);

        agentContext.setFileIds(toList(input.getForwardedProp("fileIds")));

        AccountVO userInfo = input.getForwardedProp("userInfo") != null
                ? JsonUtils.objectToBean(input.getForwardedProp("userInfo"), AccountVO.class)
                : null;
        agentContext.setUserInfo(userInfo);

        agentContext.setParams(toMap(input.getForwardedProp("params")));

        CONTEXT_HOLDER.set(agentContext);
    }

    private static Map<String, Object> toMap(Object params) {
        if (params == null) {
            return new HashMap<>();
        }

        return JsonUtils.parse(JsonUtils.toJsonStr(params), new TypeReference<Map<String, Object>>() {});
    }

    private static List<String> toList(Object params) {
        if (params == null) {
            return new ArrayList<>();
        }
        return JsonUtils.parse(JsonUtils.toJsonStr(params), new TypeReference<List<String>>() {});
    }

    public static AgentContext get() {
        AgentContext agentContext = CONTEXT_HOLDER.get();
        if (agentContext == null) {
            throw new IllegalStateException(
                    String.format("AgentContext not initialized for thread %s. " +
                                    "Please ensure init() is called before get().",
                            Thread.currentThread().getName())
            );
        }
        return agentContext;
    }

    public static Optional<AgentContext> getIfExists() {
        return Optional.ofNullable(CONTEXT_HOLDER.get());
    }

    public static void set(AgentContext agentContext) {
        CONTEXT_HOLDER.set(agentContext);
    }

    public static boolean exist() {
        return CONTEXT_HOLDER.get() != null;
    }

    public static void clean() {
        CONTEXT_HOLDER.remove();
    }
}
