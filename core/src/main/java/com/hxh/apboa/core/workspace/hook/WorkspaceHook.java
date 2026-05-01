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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 工作空间钩子（重构版），在工具调用前拦截并纠正文件路径，
 * 确保所有文件操作都被限定在会话专属的工作单元或全局技能目录内。
 *
 * <p>主要优化：
 * <ul>
 *   <li>消除重复代码，统一路径修正入口</li>
 *   <li>抽取目录常量，降低硬编码</li>
 *   <li>完善 Shell 危险模式检测，拦截更多逃逸尝试</li>
 *   <li>路径真实性校验（toRealPath）防止符号链接绕过</li>
 *   <li>日志脱敏，保护敏感信息</li>
 *   <li>增强代码可测试性和扩展性</li>
 * </ul>
 *
 * @author huxuehao
 **/
@Slf4j
public class WorkspaceHook implements Hook {

    // ==================== 目录结构常量 ====================
    private static final String WORKSPACE_RELATIVE = SysConst.WORKSPACE_PATH;
    private static final String SKILLS_DIR_NAME = SysConst.SKILLS_DIR_NAME;
    private static final String UNITS_DIR_NAME = SysConst.UNITS_DIR_NAME;

    // 需要路径检查的工具集（可配置化，未来可改为从外部注入）
    private static final Set<String> PATH_SENSITIVE_TOOLS = new HashSet<>(Arrays.asList(
            "view_text_file",
            "list_directory",
            "insert_text_file",
            "write_text_file",
            "execute_shell_command"
    ));

    // 工具参数名映射：工具名 -> 路径参数名，以及是否只读（允许访问技能目录）
    private static final Map<String, ToolPathMeta> TOOL_PATH_META = new HashMap<>();
    static {
        TOOL_PATH_META.put("view_text_file",   new ToolPathMeta("file_path", true));
        TOOL_PATH_META.put("list_directory",   new ToolPathMeta("dir_path",  true));
        TOOL_PATH_META.put("insert_text_file", new ToolPathMeta("file_path", false));
        TOOL_PATH_META.put("write_text_file",  new ToolPathMeta("file_path", false));
        // execute_shell_command 特殊处理，不在通用映射里
    }

    // Shell 危险模式：更全面的匹配，覆盖 ${}, $(), ``, $variable, $(( )), $(< ), ${! } 等
    private static final Pattern DANGEROUS_SHELL_PATTERN =
            Pattern.compile("\\$(\\{|\\()|`[^`]*`|\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\(<[^)]*\\)|\\$\\(\\([^)]*\\)\\)");

    // 纯命令名正则
    private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    // 路径分隔符检测
    private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("[/\\\\]");

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent preActing) {
            String threadId = extractThreadId(event);
            if (threadId == null) {
                log.error("无法获取 Agent 上下文的 threadId，跳过路径检查（安全失败）");
                return Mono.just(event);
            }

            // 确保工作单元目录存在（原有逻辑保留）
            FolderUtils.mkdirsByRelativePath(String.format("%s/%s/%s", SysConst.WORKSPACE_PATH, UNITS_DIR_NAME, threadId));

            ToolUseBlock toolUse = preActing.getToolUse();
            if (toolUse != null && PATH_SENSITIVE_TOOLS.contains(toolUse.getName())) {
                ToolUseBlock corrected = correctToolUse(toolUse, threadId);
                preActing.setToolUse(corrected);
            }
        }
        return Mono.just(event);
    }

    /**
     * 从事件中提取 threadId（即 sessionId）
     */
    private String extractThreadId(HookEvent event) {
        if (event.getAgent() instanceof AgentBase agentBase) {
            return AgentMetadataStore.get(agentBase.getAgentId(), "threadId");
        }
        return null;
    }

    /**
     * 统一工具调用修正路由
     */
    private ToolUseBlock correctToolUse(ToolUseBlock toolUse, String threadId) {
        if ("execute_shell_command".equals(toolUse.getName())) {
            return correctShellCommand(toolUse, threadId);
        }

        ToolPathMeta meta = TOOL_PATH_META.get(toolUse.getName());
        if (meta != null) {
            return correctFilePath(toolUse, threadId, meta.paramName, meta.allowSkills);
        }

        return toolUse;
    }

    // ==================== 通用文件路径修正 ====================
    private ToolUseBlock correctFilePath(ToolUseBlock original, String threadId,
                                         String paramName, boolean allowSkills) {
        Map<String, Object> input = original.getInput();
        Object rawPath = input.get(paramName);
        if (rawPath == null) return original;

        String originalPath = rawPath.toString();
        String correctedPath = resolveAndValidatePath(originalPath, threadId, paramName, allowSkills);

        if (correctedPath.equals(originalPath)) {
            return original;
        }

        return buildNewToolUse(original, paramName, correctedPath);
    }

    /**
     * 路径解析与校验核心逻辑
     *
     * @param rawPath     原始路径
     * @param threadId    会话 ID
     * @param paramName   参数名（用于日志）
     * @param allowSkills 是否允许指向技能目录
     * @return 安全的绝对路径
     */
    private String resolveAndValidatePath(String rawPath, String threadId,
                                          String paramName, boolean allowSkills) {
        if (rawPath == null || rawPath.isBlank()) {
            return rawPath;
        }

        Path unitDir = getUnitBaseDir(threadId);
        Path skillsDir = getSkillsDir();

        Path inputPath = Paths.get(rawPath);
        Path resolved;

        // 相对路径：基于工作单元目录解析
        if (!inputPath.isAbsolute()) {
            resolved = unitDir.resolve(inputPath).normalize();
            log.debug("路径解析(相对): {}='{}' -> '{}'", paramName, sanitize(rawPath), sanitize(resolved.toString()));
        } else {
            resolved = inputPath.toAbsolutePath().normalize();
        }

        // 1. 是否在工作单元内
        if (isInside(resolved, unitDir)) {
            return resolved.toString();
        }

        // 2. 若允许只读，检查是否在技能目录内
        if (allowSkills && isInside(resolved, skillsDir)) {
            return resolved.toString();
        }

        // 3. 写操作试图访问技能目录 → 拦截并重定向
        if (!allowSkills && isInside(resolved, skillsDir)) {
            log.warn("写操作被拒绝访问技能目录: {}='{}'", paramName, sanitize(rawPath));
            return redirectToUnitDir(resolved, unitDir, rawPath);
        }

        // 4. 逃逸到外部 → 提取文件名重定向
        log.warn("路径越权已拦截: {}='{}'", paramName, sanitize(rawPath));
        return redirectToUnitDir(resolved, unitDir, rawPath);
    }

    /**
     * 将非法路径重定向到工作单元根目录或该文件名
     */
    private String redirectToUnitDir(Path resolved, Path unitDir, String rawPath) {
        Path fileName = resolved.getFileName();
        if (fileName != null) {
            Path redirected = unitDir.resolve(fileName.toString()).normalize();
            log.warn("路径被重定向: '{}' -> '{}'", sanitize(rawPath), sanitize(redirected.toString()));
            return redirected.toString();
        }
        log.warn("无法提取文件名，回退到工作单元根目录: raw='{}'", sanitize(rawPath));
        return unitDir.toString();
    }

    /**
     * 更严格的范围检查：使用 toRealPath 防止符号链接绕过（若文件存在）
     */
    private boolean isInside(Path path, Path base) {
        try {
            Path realPath = Files.exists(path) ? path.toRealPath() : path.toAbsolutePath().normalize();
            Path realBase = base.toRealPath();
            return realPath.startsWith(realBase);
        } catch (Exception e) {
            // 如果 toRealPath 失败（比如路径不存在），回退到 normalize 判断
            log.debug("toRealPath 失败，回退到 normalize 检查: {}", e.getMessage());
            return path.toAbsolutePath().normalize().startsWith(base.toAbsolutePath().normalize());
        }
    }

    // ==================== Shell 命令特殊处理 ====================
    private ToolUseBlock correctShellCommand(ToolUseBlock original, String threadId) {
        Map<String, Object> input = original.getInput();
        Object rawCommand = input.get("command");
        if (rawCommand == null || rawCommand.toString().isBlank()) {
            return original;
        }

        String command = rawCommand.toString().trim();

        // 全局危险模式扫描
        if (containsDangerousShellConstructs(command)) {
            log.warn("Shell 命令包含危险展开，已阻止: '{}'", sanitize(command));
            String safeCmd = "echo '[BLOCKED] Command contains unsafe expansions'";
            return buildNewToolUse(original, "command", safeCmd);
        }

        String resolvedCmd = resolveCommandPaths(command, threadId);
        if (resolvedCmd.equals(command)) {
            return original;
        }

        log.debug("Shell 命令路径已修正: '{}' -> '{}'", sanitize(command), sanitize(resolvedCmd));
        return buildNewToolUse(original, "command", resolvedCmd);
    }

    /**
     * 解析命令中所有路径 token
     */
    private String resolveCommandPaths(String command, String threadId) {
        Path unitDir = getUnitBaseDir(threadId);
        Path skillsDir = getSkillsDir();

        List<String> tokens = shellTokenize(command);
        boolean changed = false;
        List<String> resolvedTokens = new ArrayList<>(tokens.size());

        for (String token : tokens) {
            String resolved = resolveShellToken(token, unitDir, skillsDir);
            if (!resolved.equals(token)) {
                changed = true;
            }
            resolvedTokens.add(resolved.contains(" ") ? "\"" + resolved + "\"" : resolved);
        }

        return changed ? String.join(" ", resolvedTokens) : command;
    }

    /**
     * 解析单个 Shell token：选项、命令名、路径等
     */
    private String resolveShellToken(String token, Path unitDir, Path skillsDir) {
        if (token == null || token.isEmpty()) return token;

        // 危险模式再次检测（双重保险）
        if (containsDangerousShellConstructs(token)) {
            log.warn("Shell token 包含危险展开，已替换: '{}'", sanitize(token));
            return "\"/dev/null\"";
        }

        // 选项：保留
        if (token.startsWith("-")) return token;

        // 纯命令名：保留
        if (COMMAND_NAME_PATTERN.matcher(token).matches()) return token;

        // 判断是否为路径
        if (!PATH_SEPARATOR_PATTERN.matcher(token).find() && !token.contains(".")) {
            return token; // 既不是路径也不是选项，可能是普通字符串
        }

        // 路径处理
        String normalizedToken = token.replace('\\', '/');

        // skills/ 前缀快捷方式
        if (normalizedToken.startsWith(SKILLS_DIR_NAME + "/")) {
            String rel = normalizedToken.substring(SKILLS_DIR_NAME.length() + 1);
            Path resolved = skillsDir.resolve(rel).normalize();
            if (resolved.startsWith(skillsDir)) {
                log.debug("Shell 技能路径解析: '{}' -> '{}'", token, sanitize(resolved.toString()));
                return resolved.toString();
            }
            log.warn("Shell 技能路径逃逸已拦截: '{}'", sanitize(token));
            return unitDir.toString();
        }

        Path inputPath = Paths.get(token);
        Path resolved;

        if (!inputPath.isAbsolute()) {
            resolved = unitDir.resolve(inputPath).normalize();
        } else {
            resolved = inputPath.toAbsolutePath().normalize();
        }

        // 范围检查
        if (isInside(resolved, unitDir) || isInside(resolved, skillsDir)) {
            log.debug("Shell 路径在允许范围内: '{}'", sanitize(token));
            return resolved.toString();
        }

        // 逃逸处理
        return redirectToUnitDir(resolved, unitDir, token);
    }

    // ==================== Shell 分词器 ====================
    /**
     * 规则：
     * - 支持双引号和单引号（引号内的内容作为一个 token）
     * - 反斜杠在引号外为普通字符（保留 Windows 路径分隔符）
     * - 反斜杠在引号内作为转义符（用于 \"、\\ 等）
     * - 空格在引号外作为分隔符
     */
    private List<String> shellTokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quoteChar = 0;   // 0: 不在引号内, '"': 双引号, '\'': 单引号
        boolean escape = false;   // 仅在引号内有效

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);

            if (quoteChar != 0) {
                // ---------- 在引号内 ----------
                if (escape) {
                    // 转义后的字符直接添加
                    current.append(ch);
                    escape = false;
                    continue;
                }
                if (ch == '\\') {
                    // 引号内的反斜杠开启转义状态
                    escape = true;
                    continue;
                }
                if (ch == quoteChar) {
                    // 引号闭合，保存 token
                    tokens.add(current.toString());
                    current.setLength(0);
                    quoteChar = 0;
                } else {
                    current.append(ch);
                }
            } else {
                // ---------- 不在引号内 ----------
                if (ch == '"' || ch == '\'') {
                    // 进入引号状态，先前积累的普通内容作为一个 token
                    if (!current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    quoteChar = ch;
                } else if (Character.isWhitespace(ch)) {
                    // 空白分隔
                    if (!current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    // 普通字符（包括反斜杠）直接添加
                    current.append(ch);
                }
            }
        }

        // 收尾
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    // ==================== 工具辅助方法 ====================
    private ToolUseBlock buildNewToolUse(ToolUseBlock original, String key, String newValue) {
        Map<String, Object> newInput = new HashMap<>(original.getInput());
        newInput.put(key, newValue);
        return ToolUseBlock.builder()
                .id(original.getId())
                .name(original.getName())
                .input(newInput)
                .content(original.getContent())
                .metadata(original.getMetadata())
                .build();
    }

    private boolean containsDangerousShellConstructs(String command) {
        return DANGEROUS_SHELL_PATTERN.matcher(command).find();
    }

    // ==================== 目录获取 ====================
    private Path getUnitBaseDir(String threadId) {
        Objects.requireNonNull(threadId, "threadId 不能为 null");
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, WORKSPACE_RELATIVE, UNITS_DIR_NAME, threadId)
                .toAbsolutePath()
                .normalize();
    }

    private Path getSkillsDir() {
        String currentDir = System.getProperty("user.dir");
        return Paths.get(currentDir, WORKSPACE_RELATIVE, SKILLS_DIR_NAME)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 日志脱敏：仅显示文件名或截断路径
     */
    private String sanitize(String path) {
        if (path == null) return null;
        if (path.length() > 60) {
            return "..." + path.substring(path.length() - 50);
        }
        return path;
    }

    // ==================== 内部元数据类 ====================
    private static class ToolPathMeta {
        final String paramName;
        final boolean allowSkills;

        ToolPathMeta(String paramName, boolean allowSkills) {
            this.paramName = paramName;
            this.allowSkills = allowSkills;
        }
    }
}
