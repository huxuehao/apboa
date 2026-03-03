package com.hxh.apboa.core.agui;

import com.hxh.apboa.common.entity.AgentDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

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
    private AgentDefinition agentDefinition;

    public static AgentContext get() {
        AgentContext agentContext = CONTEXT_HOLDER.get();
        if (agentContext == null) {
            agentContext = new AgentContext();
            CONTEXT_HOLDER.set(agentContext);
        }

        return CONTEXT_HOLDER.get();
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
