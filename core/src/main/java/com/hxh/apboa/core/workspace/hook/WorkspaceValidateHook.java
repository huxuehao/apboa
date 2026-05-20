package com.hxh.apboa.core.workspace.hook;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.util.AgentMetadataStore;
import com.hxh.apboa.common.util.FolderUtils;
import com.hxh.apboa.security.script.ScriptSecurityService;
import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.SecurityFinding;
import com.hxh.apboa.security.script.model.SecurityReport;
import com.hxh.apboa.security.script.model.Severity;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.message.ToolUseBlock;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 描述：工作空间路径验证钩子，验证路径以及脚本内容，确保所有路径都被限定在会话专属的工作单元或全局技能目录内
 * <p>
 * 作为 Hook 生命周期入口，编排以下验证器完成安全校验：
 * <ul>
 *   <li>{@link PathValidator} —— 路径合法性校验</li>
 *   <li>{@link ShellValidator} —— Shell 命令安全校验</li>
 * </ul>
 *
 * @author huxuehao
 **/
@Slf4j
public class WorkspaceValidateHook implements Hook {
    /**
     * 匹配解释器内联代码调用的正则：python -c "..."、node -e "..."、bash -c "..." 等
     * Group 1: 解释器名（python、node、bash 等）
     * Group 2: 标志位（-c、-e、-r、--eval）
     * Group 3: 引号内的代码内容
     */
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile(
            "(python3?|python|node|nodejs|bash|sh|zsh|perl|ruby|php)\\s+" +
                    "(-[cer]|--eval)\\s+" +
                    "(\"([^\"]*)\"|'([^']*)')",
            Pattern.CASE_INSENSITIVE);

    /** 解释器名 → ScriptType 映射 */
    private static final Map<String, ScriptType> INTERPRETER_TYPE_MAP = Map.of(
            "python", ScriptType.PYTHON,
            "python3", ScriptType.PYTHON,
            "node", ScriptType.NODEJS,
            "nodejs", ScriptType.NODEJS,
            "bash", ScriptType.SHELL,
            "sh", ScriptType.SHELL,
            "zsh", ScriptType.SHELL
    );

    private final PathValidator pathValidator = new PathValidator();
    private final ShellValidator shellValidator = new ShellValidator(pathValidator);
    private final ScriptSecurityService scriptSecurity = new ScriptSecurityService();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent preActing) {
            ToolUseBlock toolUse = preActing.getToolUse();
            if (toolUse != null && ToolConstants.PATH_SENSITIVE_TOOLS.contains(toolUse.getName())) {
                // 获取当前会话的 threadId
                String threadId = extractThreadId(event);
                if (threadId == null) {
                    return Mono.error(new RuntimeException("Unable to obtain threadId from Agent context"));
                }

                // 确保工作单元目录存在
                FolderUtils.mkdirsByRelativePath(String.format("%s/%s", SysConst.WORKSPACE_PATH, threadId));
                try {
                    validateToolUse(toolUse);
                } catch (Exception e) {
                    preActing.setToolUse(buildErrorToolUse(toolUse, e.getMessage()));
                }
            }
        }

        return Mono.just(event);
    }

    /**
     * 根据工具名称将校验请求路由至对应的验证器
     *
     * @param toolUse 工具调用请求
     */
    private void validateToolUse(ToolUseBlock toolUse) {
        String name = toolUse.getName();
        Map<String, Object> input = toolUse.getInput();

        switch (name) {
            case "list_directory":
                pathValidator.validatePathParam(input, "dir_path", false);
                break;
            case "view_text_file":
            case "insert_text_file":
            case "write_text_file":
            case "search_replace_file":
                pathValidator.validatePathParam(input, "file_path", false);
                checkScriptFile(input, "file_path", "content");
                break;
            case "execute_shell_command":
                shellValidator.validateShellCommand(input);
                checkInlineCodeSecurity(input);
                break;
            default:
                break;
        }
    }

    /**
     * 检查脚本文件内容
     *
     * @param input             工具调用参数
     * @param filePathParamName 文件路径参数名
     * @param contentParamName  文件内容参数名
     */
    private void checkScriptFile(Map<String, Object> input, String filePathParamName, String contentParamName) {
        if (input.get(filePathParamName) != null && input.get(contentParamName) != null) {
            String fileName = (String) input.get(filePathParamName);
            String content = (String) input.get(contentParamName);
            SecurityReport report = scriptSecurity.check(fileName, content);
            if (!report.safe()) {
                throw new RuntimeException(buildSecurityViolationMessage(report));
            }
        }
    }
    /**
     * 检测 Shell 命令中的解释器内联代码，并送入 ScriptSecurityService 做语义安全分析
     * <p>
     * 覆盖场景：AI 通过 python -c / node -e / bash -c 等方式在 shell 中内联执行危险代码，
     * 这类调用不含 $(...) 等 shell 动态求值符号，因此 DANGEROUS_SHELL_PATTERN 无法捕获。
     *
     * @param input 工具输入参数，需包含 "command" 字段
     */
    private void checkInlineCodeSecurity(Map<String, Object> input) {
        Object raw = input.get("command");
        if (raw == null) return;
        String command = raw.toString().trim();

        Matcher matcher = INLINE_CODE_PATTERN.matcher(command);
        while (matcher.find()) {
            String interpreter = matcher.group(1).toLowerCase();
            // 提取代码内容（双引号组或单引号组）
            String code = matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
            if (code == null || code.isBlank()) continue;

            ScriptType type = INTERPRETER_TYPE_MAP.get(interpreter);
            if (type == null) {
                // 不支持的解释器（perl/ruby/php），跳过 AST 语义分析，
                // 但 ShellSecurityChecker 仍可通过 DANGEROUS_SHELL_PATTERN 拦截 shell 层面的危险
                log.debug("Unsupported interpreter for inline code check: {}", interpreter);
                continue;
            }

            // 构造虚拟文件名以匹配类型识别（如 inline.py、inline.js）
            String virtualFileName = "inline." + extractExtension(type);
            SecurityReport report = scriptSecurity.check(virtualFileName, code);

            if (!report.safe()) {
                throw new RuntimeException(buildSecurityViolationMessage(report));
            }
        }
    }

    /**
     * 根据 ScriptType 获取对应的文件扩展名
     */
    private static String extractExtension(ScriptType type) {
        return type.getExtensions().stream().findFirst().orElse("txt");
    }


    /**
     * 根据安全检查报告构建面向 AI 的违规报告与行为指引
     *
     * @param report 安全检查报告
     * @return 面向 AI 的结构化英文消息
     */
    private String buildSecurityViolationMessage(SecurityReport report) {
        StringBuilder msg = new StringBuilder();

        // ========== 头部：阻断声明 ==========
        msg.append("SECURITY VIOLATION DETECTED — This operation has been BLOCKED.\n");
        msg.append("=".repeat(64)).append("\n\n");
        msg.append(String.format("File: %s\n", report.fileName()));
        msg.append(String.format("Type: %s\n", report.scriptType() != null ? report.scriptType().getTypeName() : "unknown"));
        msg.append(String.format("Total findings: %d\n", report.totalCount()));

        // ========== 严重等级分布 ==========
        Map<Severity, Long> severityDist = report.severityDistribution();
        if (!severityDist.isEmpty()) {
            msg.append("Severity breakdown: ");
            msg.append(severityDist.entrySet().stream()
                    .sorted((a, b) -> b.getKey().ordinal() - a.getKey().ordinal())
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(", ")));
            msg.append("\n");
        }

        // ========== 分类分布 ==========
        Map<FindingCategory, Long> categoryDist = report.categoryDistribution();
        if (!categoryDist.isEmpty()) {
            msg.append("Categories: ");
            msg.append(categoryDist.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .map(e -> e.getKey().getLabel() + ":" + e.getValue())
                    .collect(Collectors.joining(", ")));
            msg.append("\n");
        }

        // ========== 详细发现列表（最多列出前8条 CRITICAL/HIGH） ==========
        List<SecurityFinding> criticalAndHigh = report.findings().stream()
                .filter(f -> f.severity() == Severity.CRITICAL || f.severity() == Severity.HIGH)
                .limit(8)
                .toList();

        if (!criticalAndHigh.isEmpty()) {
            msg.append("\n--- Key Findings ---\n");
            for (int i = 0; i < criticalAndHigh.size(); i++) {
                SecurityFinding f = criticalAndHigh.get(i);
                msg.append(String.format("\n#%d [%s] %s (Line %d)\n",
                        i + 1, f.severity(), f.ruleId(), f.lineNumber()));
                msg.append(String.format("  Category: %s\n", f.category().getLabel()));
                msg.append(String.format("  Issue: %s\n", f.description()));
                if (f.matchedContent() != null && !f.matchedContent().isBlank()) {
                    String snippet = f.matchedContent().length() > 120
                            ? f.matchedContent().substring(0, 120) + "..."
                            : f.matchedContent();
                    msg.append(String.format("  Matched: %s\n", snippet));
                }
            }
        }

        // ========== AI 行为指引 ==========
        msg.append("\n").append("=".repeat(64)).append("\n");
        msg.append("INSTRUCTIONS FOR YOU (the AI assistant):\n");
        msg.append("=".repeat(64)).append("\n\n");
        msg.append("1. DO NOT attempt to write, modify, or create this file again.\n");
        msg.append("2. DO NOT attempt to bypass this check. This includes, but is not limited to:\n");
        msg.append("   - Changing the file extension\n");
        msg.append("   - Encoding or obfuscating the content (Base64, hex, etc.)\n");
        msg.append("   - Splitting strings or spreading the operation across multiple steps\n");
        msg.append("   - Using alternative tools or methods to achieve the same goal\n");
        msg.append("3. This block is FINAL and cannot be appealed within this conversation.\n");
        msg.append("\n");
        msg.append("HOW TO RESPOND TO THE USER:\n");
        msg.append("- Politely inform the user that their request cannot be completed because it triggered\n");
        msg.append("  the script security protection system.\n");
        msg.append("- Briefly summarize the security concerns identified without listing every rule match.\n");
        msg.append("- Use language like: ");
        msg.append("\"I'm unable to complete this request because it contains patterns that\n");
        msg.append("  are blocked by our security policy.");
        msg.append(" This policy exists to maintain a safe and reliable\n");
        msg.append("  environment for everyone. I appreciate your understanding.\"\n");
        msg.append("- DO NOT suggest workarounds or alternatives that bypass the security restriction.\n");
        msg.append("- DO NOT reveal the exact matching rules or detection patterns used.\n");
        msg.append("- If the user insists, remain firm but polite. The decision is not negotiable.\n");

        return msg.toString();
    }

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
