package com.hxh.apboa.core.workspace.hook;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：工作空间脚本内容安全验证器
 * <p>
 * 负责校验写入脚本文件（.sh/.py/.js 等）的内容中是否包含：
 * <ul>
 *   <li>绝对路径引用</li>
 *   <li>非法的 .. 路径逃逸</li>
 * </ul>
 * 通过逐行扫描引号内内容及空白分隔 token 来检测违规路径。
 *
 * @author huxuehao
 **/
public class WorkspaceScriptValidator {

    private final WorkspacePathValidator pathValidator;

    /** 引号内内容匹配模式 */
    private static final Pattern QUOTED_CONTENT_PATTERN = Pattern.compile("[\"']([^\"']*)[\"']");

    public WorkspaceScriptValidator(WorkspacePathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    /**
     * 对写入操作进行综合校验：先校验目标路径，若为脚本文件则进一步校验内容
     *
     * @param input     工具输入参数
     * @param pathParam 路径参数名
     * @throws WorkspaceSecurityException 路径或内容不合法时抛出
     */
    public void validatePathAndContent(Map<String, Object> input, String pathParam) {
        // 1. 路径本身验证（禁止任何 ..）
        Object rawPath = input.get(pathParam);
        if (rawPath != null) {
            pathValidator.validateRelativePath(rawPath.toString(), pathParam, false);
        }

        // 2. 如果路径是脚本文件，验证 content 中的路径
        if (rawPath != null && pathValidator.isScriptFile(rawPath.toString())) {
            Object rawContent = input.get("content");
            if (rawContent != null) {
                validateScriptContent(rawContent.toString());
            }
        }
    }

    /**
     * 逐行检测脚本内容中的绝对路径与非法 ..
     * <p>
     * 检测策略：
     * <ol>
     *   <li>跳过注释行（# 或 // 开头）</li>
     *   <li>提取引号内字符串并检测路径违规</li>
     *   <li>检测空白分隔的 token 中是否包含路径违规</li>
     * </ol>
     *
     * @param content 脚本文件内容
     * @throws WorkspaceSecurityException 内容包含违规路径时抛出
     */
    public void validateScriptContent(String content) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            // 忽略注释行
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue;

            // 匹配引号内内容
            Matcher m = QUOTED_CONTENT_PATTERN.matcher(line);
            while (m.find()) {
                String candidate = m.group(1);
                if (pathValidator.isPathViolation(candidate)) {
                    throw new WorkspaceSecurityException(
                            "脚本内容中包含违规路径 '" + candidate + "'，请确保所有路径为相对路径且无逃逸。");
                }
            }

            // 无引号 token 检查
            String[] tokens = line.split("\\s+");
            for (String token : tokens) {
                if (token.contains("/") && pathValidator.isPathViolation(token)) {
                    throw new WorkspaceSecurityException(
                            "脚本内容中发现违规路径 '" + token + "'，禁止绝对路径或非法 '..'。");
                }
            }
        }
    }
}
