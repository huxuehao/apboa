package com.hxh.apboa.core.workspace.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.hxh.apboa.core.workspace.hook.ToolConstants.ALLOWED_SKILLS_PREFIX;
import static com.hxh.apboa.core.workspace.hook.ToolConstants.COMMAND_NAME_PATTERN;
import static com.hxh.apboa.core.workspace.hook.ToolConstants.DANGEROUS_SHELL_PATTERN;

/**
 * 描述：Shell 命令安全验证器
 * <p>
 * 负责多维度校验 Shell 命令的安全性：
 * <ul>
 *   <li>禁止危险模式（变量展开、命令替换、进程替换等）</li>
 *   <li>分词后逐 token 检测路径逃逸（允许合法的 ../../skills/ 前缀）</li>
 *   <li>检测内联代码中的路径违规（-e "..."、-c "..." 等）</li>
 * </ul>
 *
 * @author huxuehao
 **/
public class ShellValidator {

    private final PathValidator pathValidator;

    public ShellValidator(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    /**
     * 对 Shell 命令进行多维安全校验
     *
     * @param input 工具输入参数，需包含 "command" 字段
     * @throws WorkspaceSecurityException 命令不安全时抛出
     */
    public void validateShellCommand(Map<String, Object> input) {
        Object raw = input.get("command");
        if (raw == null) return;
        String command = raw.toString().trim();

        // 1. 危险模式检测
        if (DANGEROUS_SHELL_PATTERN.matcher(command).find()) {
            throw new WorkspaceSecurityException(
                    "Shell command contains unsafe expansions or substitutions, which are prohibited. Please use simple commands without dynamic evaluation.");
        }

        // 2. 分词后逐 token 检测
        List<String> tokens = shellTokenize(command);
        for (String token : tokens) {
            // 跳过选项标志（-x）和合法命令名
            if (token.startsWith("-")) continue;
            if (COMMAND_NAME_PATTERN.matcher(token).matches()) continue;

            // 可能为路径的 token
            if (token.contains("/") || token.contains("\\") || token.contains(".")) {
                // Shell 命令允许 ../../skills/ 前缀
                pathValidator.validateRelativePath(token, "command", true);
            }

            // 内联代码检测 (-e "...", -c "...")
            if (isInlineCodeToken(token)) {
                String code = extractInlineCode(token);
                if (code != null) {
                    validateInlineCodeForPaths(code);
                }
            }
        }
    }

    /**
     * Shell 命令分词器
     * <p>
     * 支持引号包裹的参数（单引号/双引号）以及转义字符，正确处理空格分隔。
     *
     * @param command 原始命令字符串
     * @return 分词后的 token 列表
     */
    public List<String> shellTokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escape = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (quote != 0) {
                if (escape) {
                    current.append(c);
                    escape = false;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == quote) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    quote = 0;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    if (!current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    quote = c;
                } else if (Character.isWhitespace(c)) {
                    if (!current.isEmpty()) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /**
     * 检测 token 是否为内联代码片段（如 -e "print('hello')"）
     */
    private boolean isInlineCodeToken(String token) {
        return token.startsWith("-e ") || token.startsWith("-c ")
                || token.startsWith("-e\"") || token.startsWith("-e'")
                || token.startsWith("-c\"") || token.startsWith("-c'");
    }

    /**
     * 从内联代码 token 中提取代码内容
     *
     * @param token 内联代码 token
     * @return 提取的代码字符串，若无法提取则返回 null
     */
    private String extractInlineCode(String token) {
        int idx1 = token.indexOf('"');
        int idx2 = token.indexOf('\'');
        int idx = (idx1 == -1) ? idx2 : (idx2 == -1 ? idx1 : Math.min(idx1, idx2));
        if (idx == -1) return null;
        char quote = token.charAt(idx);
        int end = token.indexOf(quote, idx + 1);
        if (end == -1) return null;
        return token.substring(idx + 1, end);
    }

    /**
     * 内联代码路径安全检测
     * <p>
     * 检测策略：
     * <ol>
     *   <li>盘符路径（如 D:\、C:/）→ 拒绝</li>
     *   <li>Unix 绝对路径（以 / 开头）→ 拒绝</li>
     *   <li>.. 路径逃逸（仅允许 ../../skills/ 前缀）</li>
     *   <li>逐 token 扫描（空白/分号分隔）</li>
     * </ol>
     *
     * @param code 内联代码内容
     * @throws WorkspaceSecurityException 代码中包含违规路径时抛出
     */
    private void validateInlineCodeForPaths(String code) {
        if (code == null || code.isBlank()) return;

        // 1. 检测盘符路径（如 D:\, C:/, D:\\ 等）
        Pattern drivePattern = Pattern.compile("[a-zA-Z]:[/\\\\]");
        if (drivePattern.matcher(code).find()) {
            throw new WorkspaceSecurityException(
                    "Drive letter path detected in inline code (e.g., 'D:\\' or 'C:/'), absolute paths are prohibited. Please use relative paths.");
        }

        // 2. 检测以 / 开头的绝对路径
        if (code.startsWith("/") || Pattern.compile("\\s/").matcher(code).find()) {
            throw new WorkspaceSecurityException(
                    "Absolute path (starting with /) detected in inline code. Please use relative paths.");
        }

        // 3. 检测 .. 逃逸，但允许 ../../skills/ 前缀
        int idx = 0;
        while ((idx = code.indexOf("..", idx)) != -1) {
            boolean isAllowed = idx >= 2 && code.startsWith(ALLOWED_SKILLS_PREFIX, idx - 2);
            if (!isAllowed) {
                throw new WorkspaceSecurityException(
                        "Path escape '..' detected in inline code. Only skill scripts may use ../../skills/ prefix.");
            }
            idx += 2;
        }

        // 4. 逐 token 扫描（空白/分号分隔）
        String[] parts = code.split("[\\s;]+");
        for (String part : parts) {
            // 去掉引号（单引号或双引号）
            String cleaned = part.replaceAll("^[\"']|[\"']$", "");
            if (cleaned.startsWith("/") || cleaned.matches("^[a-zA-Z]:[/\\\\].*")) {
                throw new WorkspaceSecurityException(
                        "Absolute path '" + cleaned + "' detected in inline code. Please use relative paths.");
            }
            if (cleaned.contains("..") && !cleaned.startsWith(ALLOWED_SKILLS_PREFIX)) {
                throw new WorkspaceSecurityException(
                        "Path escape '" + cleaned + "' detected in inline code. Only skill scripts may use ../../skills/ prefix.");
            }
        }
    }
}
