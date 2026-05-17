package com.hxh.apboa.core.workspace.hook;

import com.hxh.apboa.common.consts.SysConst;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 描述：工具校验常量定义
 *
 * @author huxuehao
 **/
public final class ToolConstants {

    private ToolConstants() {
        // 工具类，禁止实例化
    }

    /** 技能目录名称 */
    public static final String SKILLS_DIR_NAME = SysConst.SKILLS_DIR_NAME;

    /** Shell 中唯一允许的 .. 前缀：../../skills/ */
    public static final String ALLOWED_SKILLS_PREFIX = "../../" + SKILLS_DIR_NAME + "/";

    /** 需要拦截进行路径校验的工具名称集合 */
    public static final Set<String> PATH_SENSITIVE_TOOLS = new HashSet<>(Arrays.asList(
            "view_text_file",
            "list_directory",
            "insert_text_file",
            "write_text_file",
            "search_replace_file",
            "execute_shell_command"
    ));

    /** 脚本文件后缀集合（用于判断是否需要校验文件内容中的路径） */
    public static final Set<String> SCRIPT_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".sh", ".bash", ".zsh", ".py", ".js", ".ts", ".rb", ".pl", ".php", ".ps1", ".bat", ".cmd"
    ));

    /** 禁止的 Shell 危险模式（变量展开、命令替换等） */
    public static final Pattern DANGEROUS_SHELL_PATTERN =
            Pattern.compile("\\$([{(])|`[^`]*`|\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\(<[^)]*\\)|\\$\\(\\([^)]*\\)\\)");

    /** 合法的命令名模式 */
    public static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]*$");
}
