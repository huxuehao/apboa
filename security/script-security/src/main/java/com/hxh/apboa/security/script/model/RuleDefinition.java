package com.hxh.apboa.security.script.model;

import java.util.regex.Pattern;

/**
 * 描述：安全检查规则定义，一条规则对应一类不安全行为模式
 *
 * @param id          规则唯一标识（如 "SH-001", "PY-AST-005"）
 * @param severity    严重等级
 * @param category    不安全行为分类
 * @param pattern     用于匹配的正则表达式
 * @param description 规则的人类可读描述
 * @param suggestion  修复或缓解建议
 * @author huxuehao
 */
public record RuleDefinition(
        String id,
        Severity severity,
        FindingCategory category,
        Pattern pattern,
        String description,
        String suggestion
) {

    /**
     * 创建规则定义的便捷工厂方法
     */
    public static RuleDefinition of(String id, Severity severity, FindingCategory category,
                                     String regex, String description, String suggestion) {
        return new RuleDefinition(id, severity, category, Pattern.compile(regex), description, suggestion);
    }

    /**
     * 创建带标志位的规则定义（如大小写不敏感）
     */
    public static RuleDefinition of(String id, Severity severity, FindingCategory category,
                                     String regex, int flags, String description, String suggestion) {
        return new RuleDefinition(id, severity, category, Pattern.compile(regex, flags), description, suggestion);
    }

}
