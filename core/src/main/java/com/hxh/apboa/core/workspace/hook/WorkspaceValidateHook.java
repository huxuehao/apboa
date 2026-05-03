package com.hxh.apboa.core.workspace.hook;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.util.AgentMetadataStore;
import com.hxh.apboa.common.util.FolderUtils;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.message.ToolUseBlock;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述：工作空间路径验证钩子，验证路径以及脚本内容，确保所有路径都被限定在会话专属的工作单元或全局技能目录内
 * <p>
 * 作为 Hook 生命周期入口，编排以下验证器完成安全校验：
 * <ul>
 *   <li>{@link WorkspacePathValidator} —— 路径合法性校验</li>
 *   <li>{@link WorkspaceScriptValidator} —— 脚本文件内容校验</li>
 *   <li>{@link WorkspaceShellValidator} —— Shell 命令安全校验</li>
 * </ul>
 *
 * @author huxuehao
 **/
@Slf4j
public class WorkspaceValidateHook implements Hook {

    // ==================== 组合的验证器 ====================

    private final WorkspacePathValidator pathValidator = new WorkspacePathValidator();
    private final WorkspaceScriptValidator scriptValidator = new WorkspaceScriptValidator(pathValidator);
    private final WorkspaceShellValidator shellValidator = new WorkspaceShellValidator(pathValidator);

    // ==================== Hook 生命周期 ====================

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent preActing) {
            String threadId = extractThreadId(event);
            if (threadId == null) {
                return Mono.error(new RuntimeException("无法获取 Agent 上下文的 threadId"));
            }

            // 确保工作单元目录存在
            FolderUtils.mkdirsByRelativePath(String.format("%s/%s", SysConst.WORKSPACE_PATH, threadId));

            ToolUseBlock toolUse = preActing.getToolUse();
            if (toolUse != null && WorkspaceToolConstants.PATH_SENSITIVE_TOOLS.contains(toolUse.getName())) {
                try {
                    validateToolUse(toolUse);
                } catch (Exception e) {
                    preActing.setToolUse(buildErrorToolUse(toolUse, e.getMessage()));
                }
            }
        }

        return Mono.just(event);
    }

    // ==================== 工具校验路由 ====================

    /**
     * 根据工具名称将校验请求路由至对应的验证器
     *
     * @param toolUse 工具调用请求
     */
    private void validateToolUse(ToolUseBlock toolUse) {
        String name = toolUse.getName();
        Map<String, Object> input = toolUse.getInput();

        switch (name) {
            case "view_text_file":
                pathValidator.validatePathParam(input, "file_path", false);
                break;
            case "list_directory":
                pathValidator.validatePathParam(input, "dir_path", false);
                break;
            case "insert_text_file":
            case "write_text_file":
                scriptValidator.validatePathAndContent(input, "file_path");
                break;
            case "execute_shell_command":
                shellValidator.validateShellCommand(input);
                break;
            default:
                break;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建携带校验错误信息的 ToolUseBlock
     * <p>
     * ToolExecutor 会通过 {@link SysConst#WORKSPACE_HOOK_ERROR_KEY} 判断是否存在校验错误，
     * 若存在则阻断工具执行并向 Agent 返回友好提示。
     *
     * @param original  原始工具调用
     * @param errorMsg  错误描述
     * @return 携带错误元数据的新 ToolUseBlock
     */
    private ToolUseBlock buildErrorToolUse(ToolUseBlock original, String errorMsg) {
        Map<String, Object> newMetadata = new HashMap<>(original.getMetadata());
        newMetadata.put(SysConst.WORKSPACE_HOOK_ERROR_KEY, errorMsg);
        return ToolUseBlock.builder()
                .id(original.getId())
                .name(original.getName())
                .input(original.getInput())
                .content(original.getContent())
                .metadata(newMetadata)
                .build();
    }

    /**
     * 从 Hook 事件中提取当前会话的 threadId
     *
     * @param event Hook 事件
     * @return threadId，若无法提取则返回 null
     */
    private String extractThreadId(HookEvent event) {
        if (event.getAgent() instanceof AgentBase agentBase) {
            return AgentMetadataStore.get(agentBase.getAgentId(), "threadId");
        }
        return null;
    }
}
