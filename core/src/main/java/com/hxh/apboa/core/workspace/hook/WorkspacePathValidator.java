package com.hxh.apboa.core.workspace.hook;

import java.util.Map;

import static com.hxh.apboa.core.workspace.hook.WorkspaceToolConstants.ALLOWED_SKILLS_PREFIX;
import static com.hxh.apboa.core.workspace.hook.WorkspaceToolConstants.SCRIPT_EXTENSIONS;

/**
 * 描述：工作空间路径安全验证器
 * <p>
 * 负责校验所有文件路径是否合法：
 * <ul>
 *   <li>禁止绝对路径（Unix / 开头、Windows 盘符）</li>
 *   <li>禁止路径逃逸（..），仅 Shell 场景允许 ../../skills/ 前缀</li>
 *   <li>提供脚本文件类型判断</li>
 * </ul>
 *
 * @author huxuehao
 **/
public class WorkspacePathValidator {

    /**
     * 从输入 Map 中提取路径参数并校验
     *
     * @param input             工具输入参数
     * @param paramName         参数名
     * @param allowSkillsPrefix 是否允许 ../../skills/ 前缀
     */
    public void validatePathParam(Map<String, Object> input, String paramName, boolean allowSkillsPrefix) {
        Object raw = input.get(paramName);
        if (raw == null) return;
        validateRelativePath(raw.toString(), paramName, allowSkillsPrefix);
    }

    /**
     * 校验相对路径的合法性
     * <p>
     * 规则：
     * <ol>
     *   <li>绝对路径（Unix / 开头或 Windows 盘符）→ 拒绝</li>
     *   <li>包含 .. → 仅当 allowSkillsPrefix=true 且以 ../../skills/ 开头才放行</li>
     *   <li>../../skills/ 之后不允许再有 ..</li>
     * </ol>
     *
     * @param path              待校验路径
     * @param paramName         参数名（用于错误提示）
     * @param allowSkillsPrefix 是否允许 ../../skills/ 前缀
     * @throws WorkspaceSecurityException 路径不合法时抛出
     */
    public void validateRelativePath(String path, String paramName, boolean allowSkillsPrefix) {
        if (path == null || path.isBlank()) return;
        String normalized = path.replace('\\', '/');

        // 绝对路径拒绝
        if (normalized.startsWith("/") || (normalized.length() > 2 && normalized.charAt(1) == ':')) {
            throw new WorkspaceSecurityException(
                    String.format("参数 '%s' 使用了绝对路径 '%s'，绝对路径被禁止，请使用相对路径。", paramName, path));
        }

        // 含 .. 的处理
        if (normalized.contains("..")) {
            if (allowSkillsPrefix && normalized.startsWith(ALLOWED_SKILLS_PREFIX)) {
                // 检查 ../../skills/ 之后是否还有 ..
                String after = normalized.substring(ALLOWED_SKILLS_PREFIX.length());
                if (after.contains("..")) {
                    throw new WorkspaceSecurityException(
                            String.format("参数 '%s' 的技能路径中包含多余的 '..'，逃逸被禁止：%s", paramName, path));
                }
                return; // 合法
            }
            throw new WorkspaceSecurityException(
                    String.format("参数 '%s' 使用了 '..' 进行路径逃逸：'%s'，仅 Shell 调用技能脚本时允许 ../../skills/ 前缀。",
                            paramName, path));
        }
    }

    /**
     * 检测路径是否包含绝对路径或非法 ..
     *
     * @param path 待检测路径
     * @return true 表示路径违规
     */
    public boolean isPathViolation(String path) {
        String p = path.replace('\\', '/');
        return p.startsWith("/") || (p.length() > 2 && p.charAt(1) == ':') || p.contains("..");
    }

    /**
     * 检测文件是否为脚本文件
     *
     * @param filePath 文件路径
     * @return true 表示是脚本文件
     */
    public boolean isScriptFile(String filePath) {
        String lower = filePath.toLowerCase();
        for (String ext : SCRIPT_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}
