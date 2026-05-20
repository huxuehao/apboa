package com.hxh.apboa.security.script.model;

/**
 * 描述：单条安全检查发现，记录匹配到的一处不安全行为
 *
 * @param severity       严重等级
 * @param category       不安全行为分类
 * @param ruleId         触发规则的唯一标识
 * @param description    人类可读的描述信息
 * @param matchedContent 匹配到的原始内容片段
 * @param lineNumber     行号（1-based）
 * @param columnNumber   列号（1-based），正则层无法精确定位时为0
 * @param suggestion     修复或缓解建议
 * @author huxuehao
 */
public record SecurityFinding(
        Severity severity,
        FindingCategory category,
        String ruleId,
        String description,
        String matchedContent,
        int lineNumber,
        int columnNumber,
        String suggestion
) {

    /**
     * 创建简化的发现记录（不含列号）
     */
    public static SecurityFinding of(Severity severity, FindingCategory category, String ruleId,
                                      String description, String matchedContent, int lineNumber, String suggestion) {
        return new SecurityFinding(severity, category, ruleId, description, matchedContent, lineNumber, 0, suggestion);
    }

}
