package com.hxh.apboa.security.script.ast;

import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.SecurityFinding;
import com.hxh.apboa.security.script.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：JavaScript/Node.js AST 深度分析器，在正则层之外提供语义级别的安全检查。
 * 对 eval/Function 的参数进行语义分析，区分字面量参数和变量参数；
 * 检测 require/import 的动态性；分析原型链污染；检测解码后执行的混淆模式。
 *
 * 当前版本使用增强正则 + 上下文分析实现，后续可引入 Mozilla Rhino 进行完整AST解析。
 *
 * @author huxuehao
 */
public class JavaScriptAstAnalyzer implements AstAnalyzer {

    /** 匹配 eval/Function 调用，捕获完整参数 */
    private static final Pattern EVAL_FUNCTION_PATTERN =
            Pattern.compile("(eval|new\\s+Function|Function)\\s*\\(\\s*(.+?)\\s*\\)", Pattern.DOTALL);

    /** 匹配字符串字面量 */
    private static final Pattern STRING_LITERAL =
            Pattern.compile("^\\s*['\"`]([^'\"`]*)['\"`]\\s*$");

    /** 匹配 require 使用变量的情况 */
    private static final Pattern DYNAMIC_REQUIRE =
            Pattern.compile("require\\s*\\(\\s*\\w+\\s*\\+", Pattern.DOTALL);

    /** 匹配 Buffer.from + base64 后执行 */
    private static final Pattern BUFFER_EXEC_PATTERN =
            Pattern.compile("Buffer\\.from\\s*\\([^)]+,\\s*['\"]base64['\"]\\s*\\)",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public List<SecurityFinding> analyze(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<SecurityFinding> findings = new ArrayList<>();

        // 分析 eval/Function 参数
        analyzeEvalFunction(content, findings);

        // 分析动态 require
        analyzeDynamicRequire(content, findings);

        // 分析原型链污染深度模式
        analyzePrototypePollution(content, findings);

        // 分析 Buffer.from base64 解码后执行链
        analyzeBufferExecChain(content, findings);

        return findings;
    }

    /**
     * 分析 eval/Function 的参数是否为字面量
     */
    private void analyzeEvalFunction(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = EVAL_FUNCTION_PATTERN.matcher(line);
            if (m.find()) {
                String func = m.group(1).replace("new ", "").trim();
                String args = m.group(2);

                boolean isLiteral = STRING_LITERAL.matcher(args.trim()).matches();

                if (isLiteral) {
                    findings.add(SecurityFinding.of(
                            Severity.LOW,
                            FindingCategory.CODE_EXECUTION,
                            "JS-AST-001",
                            func + "() 使用字面量参数（低风险但仍需关注）",
                            m.group().trim(),
                            i + 1,
                            "如果是固定代码请考虑直接编写，不要使用" + func
                    ));
                } else {
                    findings.add(SecurityFinding.of(
                            Severity.CRITICAL,
                            FindingCategory.CODE_EXECUTION,
                            "JS-AST-002",
                            func + "() 使用变量/表达式参数（高风险代码注入）",
                            m.group().trim(),
                            i + 1,
                            "绝对不要对不可信的输入使用" + func + "()"
                    ));
                }
            }
        }
    }

    /**
     * 分析动态 require（路径拼接）
     */
    private void analyzeDynamicRequire(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = DYNAMIC_REQUIRE.matcher(line);
            if (m.find()) {
                findings.add(SecurityFinding.of(
                        Severity.HIGH,
                        FindingCategory.COMMAND_INJECTION,
                        "JS-AST-010",
                        "require() 使用动态路径拼接",
                        m.group().trim(),
                        i + 1,
                        "动态require路径可能导致任意模块加载"
                ));
            }
        }
    }

    /**
     * 分析原型链污染深度模式
     */
    private void analyzePrototypePollution(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);

        // 检测 merge/clone/extend + __proto__ 的组合模式
        Pattern mergeProtoPattern = Pattern.compile(
                "(merge|extend|clone|assign)\\s*\\([^)]*\\w+\\s*,\\s*[^)]*__proto__",
                Pattern.CASE_INSENSITIVE);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = mergeProtoPattern.matcher(line);
            if (m.find()) {
                findings.add(SecurityFinding.of(
                        Severity.CRITICAL,
                        FindingCategory.CODE_EXECUTION,
                        "JS-AST-020",
                        m.group(1) + "() 操作中包含 __proto__ 属性（原型污染攻击）",
                        m.group().trim(),
                        i + 1,
                        "对 __proto__ 的操作可能导致原型污染RCE"
                ));
            }
        }
    }

    /**
     * 分析 Buffer.from + base64 解码后执行的混淆链
     */
    private void analyzeBufferExecChain(String content, List<SecurityFinding> findings) {
        // 跨行检测：Buffer.from base64 解码 后跟随 eval/Function/exec
        Pattern chainPattern = Pattern.compile(
                "Buffer\\.from\\s*\\([^)]+,\\s*['\"]base64['\"]\\s*\\)[\\s\\S]{0,200}(eval|Function|exec|spawn)\\s*\\(",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher m = chainPattern.matcher(content);
        while (m.find()) {
            int lineNum = countLinesBefore(content, m.start()) + 1;
            findings.add(SecurityFinding.of(
                    Severity.CRITICAL,
                    FindingCategory.OBFUSCATION,
                    "JS-AST-030",
                    "Buffer.from base64解码后执行（代码混淆执行链）",
                    truncate(m.group().trim(), 200),
                    lineNum,
                    "Base64解码后执行是恶意代码混淆的典型模式"
            ));
        }
    }

    private static int countLinesBefore(String content, int pos) {
        int count = 0;
        for (int i = 0; i < pos && i < content.length(); i++) {
            if (content.charAt(i) == '\n') count++;
        }
        return count;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    @Override
    public String supportedType() {
        return "nodejs";
    }

}
