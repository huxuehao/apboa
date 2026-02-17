package com.hxh.apboa.core.hook.impl;

import com.hxh.apboa.core.hook.IAgentHook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述：工具确认Hook
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class IConfirmationHook implements IAgentHook {
    // 需要确认的工具列表
    private static final List<String> NEED_CONFIRM_TOOLS = new ArrayList<>();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        // 监听 PostReasoningEvent
        if (event instanceof PostReasoningEvent postReasoning) {
            Msg reasoningMsg = postReasoning.getReasoningMessage();
            if (reasoningMsg == null) {
                return Mono.just(event);
            }

            // 获取工具调用列表
            List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
            if (toolCalls.isEmpty()) {
                return Mono.just(event);
            }

            // 收集需要确认的工具信息
            List<Map<String, Object>> toolsNeedConfirm = new ArrayList<>();
            for (ToolUseBlock tool : toolCalls) {
                if (NEED_CONFIRM_TOOLS.contains(tool.getName())) {
                    Map<String, Object> toolInfo = new HashMap<>();
                    toolInfo.put("id", tool.getId());
                    toolInfo.put("name", tool.getName());
                    toolInfo.put("input", convertInput(tool.getInput()));
                    toolInfo.put("dangerous", true);
                    toolsNeedConfirm.add(toolInfo);
                }
            }

            // 如果有需要确认的工具
            if (!toolsNeedConfirm.isEmpty()) {
                // TODO 发送工具确认事件
                // ...

                // 暂停 Agent 执行，等待用户确认
                postReasoning.stopAgent();
            }
        }

        return Mono.just(event);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertInput(Object input) {
        if (input == null) {
            return Map.of();
        }
        if (input instanceof Map) {
            return (Map<String, Object>) input;
        }
        // 简单类型直接包装
        return Map.of("value", input.toString());
    }

    public static void setNeedConfirmTool(String toolName) {
        if (!NEED_CONFIRM_TOOLS.contains(toolName)) {
            NEED_CONFIRM_TOOLS.add(toolName);
        }
    }

    public static void removeNeedConfirmTool(String toolName) {
        NEED_CONFIRM_TOOLS.remove(toolName);
    }

    @Override
    public String getDescription() {
        return "工具调用人工确认Hook";
    }
}
