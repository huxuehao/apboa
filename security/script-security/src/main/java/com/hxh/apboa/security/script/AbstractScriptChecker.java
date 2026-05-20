package com.hxh.apboa.security.script;

import com.hxh.apboa.security.script.model.RuleDefinition;
import com.hxh.apboa.security.script.model.SecurityFinding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：脚本安全检查器抽象基类，提供规则注册与单行/多行匹配的通用逻辑。
 * 子类只需在构造方法中调用 registerRule() 注册规则即可。
 *
 * @author huxuehao
 */
public abstract class AbstractScriptChecker implements ScriptSecurityChecker {

    /** 单行匹配规则列表 */
    private final List<RuleDefinition> singleLineRules = new ArrayList<>();

    /** 多行匹配规则列表（跨行模式检测，如下载-执行链） */
    private final List<RuleDefinition> multiLineRules = new ArrayList<>();

    /**
     * 注册单行匹配规则
     *
     * @param rule 规则定义
     */
    protected void registerRule(RuleDefinition rule) {
        singleLineRules.add(rule);
    }

    /**
     * 注册多行匹配规则
     *
     * @param rule 规则定义
     */
    protected void registerMultiLineRule(RuleDefinition rule) {
        multiLineRules.add(rule);
    }

    /**
     * 通用检查逻辑：先逐行正则匹配，再对全文进行多行模式匹配
     *
     * @param content 脚本完整内容
     * @return 发现列表
     */
    @Override
    public List<SecurityFinding> check(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<SecurityFinding> findings = new ArrayList<>();

        // 第一遍：逐行单行匹配
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (RuleDefinition rule : singleLineRules) {
                Matcher matcher = rule.pattern().matcher(line);
                while (matcher.find()) {
                    findings.add(SecurityFinding.of(
                            rule.severity(),
                            rule.category(),
                            rule.id(),
                            rule.description(),
                            matcher.group().trim(),
                            i + 1,
                            rule.suggestion()
                    ));
                }
            }
        }

        // 第二遍：全文多行匹配
        for (RuleDefinition rule : multiLineRules) {
            Matcher matcher = rule.pattern().matcher(content);
            while (matcher.find()) {
                // 计算匹配位置的行号
                int lineNumber = countNewlines(content, matcher.start()) + 1;
                findings.add(SecurityFinding.of(
                        rule.severity(),
                        rule.category(),
                        rule.id(),
                        rule.description(),
                        truncate(matcher.group().trim(), 200),
                        lineNumber,
                        rule.suggestion()
                ));
            }
        }

        return findings;
    }

    /**
     * 计算指定位置之前的换行符数量
     */
    private static int countNewlines(String content, int endIndex) {
        int count = 0;
        for (int i = 0; i < endIndex && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * 截断过长的内容
     */
    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 获取已注册的单行规则总数
     */
    protected int getSingleLineRuleCount() {
        return singleLineRules.size();
    }

    /**
     * 获取已注册的多行规则总数
     */
    protected int getMultiLineRuleCount() {
        return multiLineRules.size();
    }

}
