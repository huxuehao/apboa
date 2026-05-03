package com.hxh.apboa.core.workspace.hook;

import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.common.util.AgentMetadataStore;
import com.hxh.apboa.websocket.model.WsServerMessage;
import com.hxh.apboa.websocket.service.WebSocketPushService;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.ToolUseBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：工作空间websocket钩子
 * 目前这个类做的比较简单，但是路子是通的，后续可以扩展，精确到每一个文件。
 * 当然需要前端进行配合，接收并处理这些websocket消息
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class WorkspaceWebsocketHook implements Hook {
    private final WebSocketPushService webSocketPushService;

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostActingEvent postActingEvent) {
            String threadId = extractThreadId(event);
            if (threadId == null) {
                return Mono.just(event);
            }

            ToolUseBlock toolUse = postActingEvent.getToolUse();
            if (toolUse != null && WorkspaceToolConstants.PATH_SENSITIVE_TOOLS.contains(toolUse.getName())) {
                sendWebsocketToWorkspace(threadId, toolUse);
            }
        }

        return Mono.just(event);
    }

    private void sendWebsocketToWorkspace(String threadId, ToolUseBlock toolUse) {
        String name = toolUse.getName();

        switch (name) {
            case "insert_text_file":
            case "write_text_file":
                Map<String, Object> input = toolUse.getInput();
                sendFileChangeWs(threadId, input.get("file_path").toString());
                break;
            default:
                break;
        }
    }

    private void sendFileChangeWs(String threadId, String fileName) {
        try {
            webSocketPushService.broadcastCluster(
                    WsServerMessage.build(
                            WsMessageType.WORKSPACE_FILE_CHANGE.name(),
                            new HashMap<String, Object>() {{
                                put("fileName", fileName);
                                put("sessionId", threadId);
                            }}));
        } catch (Exception ignored) {}
    }

    private String extractThreadId(HookEvent event) {
        if (event.getAgent() instanceof AgentBase agentBase) {
            return AgentMetadataStore.get(agentBase.getAgentId(), "threadId");
        }
        return null;
    }
}
