package com.hxh.apboa.security.script.ast;

import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.SecurityFinding;
import com.hxh.apboa.security.script.model.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述：Python AST 深度分析器，在正则层之外提供语义级别的安全检查。
 * 对 eval/exec/compile 的参数进行语义分析，区分字面量参数和变量参数；
 * 追踪 import 语句和变量定义-使用链；检测经过混淆的恶意代码。
 *
 * 当前版本使用增强正则 + 上下文分析实现，后续可引入 ANTLR4 进行完整AST解析。
 *
 * @author huxuehao
 */
public class PythonAstAnalyzer implements AstAnalyzer {

    /** 匹配 eval/exec 调用，捕获完整参数 */
    private static final Pattern EVAL_EXEC_PATTERN =
            Pattern.compile("(eval|exec|compile)\\s*\\(\\s*(.+?)\\s*\\)", Pattern.DOTALL);

    /** 匹配变量被赋值为字符串拼接（混淆检测） */
    private static final Pattern CONCAT_ASSIGN_PATTERN =
            Pattern.compile("(\\w+)\\s*=\\s*['\"][^'\"]+['\"]\\s*\\+\\s*['\"][^'\"]+['\"]");

    /** 匹配字符串字面量 */
    private static final Pattern STRING_LITERAL =
            Pattern.compile("^\\s*['\"]([^'\"]*)['\"]\\s*$");

    @Override
    public List<SecurityFinding> analyze(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<SecurityFinding> findings = new ArrayList<>();

        // 分析 eval/exec/compile 参数
        analyzeEvalExec(content, findings);

        // 分析 import 语句动态性
        analyzeImports(content, findings);

        // 分析字符串拼接混淆
        analyzeStringConcat(content, findings);

        return findings;
    }

    /**
     * 分析 eval/exec/compile 的参数是否为字面量
     */
    private void analyzeEvalExec(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = EVAL_EXEC_PATTERN.matcher(line);
            if (m.find()) {
                String func = m.group(1);
                String args = m.group(2);

                // 检查参数是否是纯字符串字面量
                boolean isLiteral = STRING_LITERAL.matcher(args.trim()).matches();

                if (isLiteral) {
                    // 字面量参数 — 较低风险（但仍需关注）
                    findings.add(SecurityFinding.of(
                            Severity.LOW,
                            FindingCategory.CODE_EXECUTION,
                            "PY-AST-001",
                            func + "() 使用字面量参数（低风险但仍需关注）",
                            m.group().trim(),
                            i + 1,
                            "如果是固定代码请考虑直接编写，不要使用" + func
                    ));
                } else {
                    // 变量或表达式参数 — 高风险
                    findings.add(SecurityFinding.of(
                            Severity.CRITICAL,
                            FindingCategory.CODE_EXECUTION,
                            "PY-AST-002",
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
     * 分析 import 语句的动态性
     */
    private void analyzeImports(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检测 __import__ 调用
            if (line.contains("__import__(")) {
                findings.add(SecurityFinding.of(
                        Severity.HIGH,
                        FindingCategory.SANDBOX_ESCAPE,
                        "PY-AST-010",
                        "使用 __import__() 动态导入模块",
                        line.trim(),
                        i + 1,
                        "动态导入模块可能是沙箱逃逸尝试，请使用显式 import 语句"
                ));
            }

            // 检测 importlib.import_module 调用
            if (line.contains("import_module(")) {
                findings.add(SecurityFinding.of(
                        Severity.HIGH,
                        FindingCategory.SANDBOX_ESCAPE,
                        "PY-AST-011",
                        "使用 importlib.import_module() 动态导入",
                        line.trim(),
                        i + 1,
                        "importlib动态导入可能存在模块注入风险"
                ));
            }

            // 检测 getattr + import 组合（混淆 import）
            if (line.matches(".*getattr\\s*\\(\\s*__import__.*")) {
                findings.add(SecurityFinding.of(
                        Severity.CRITICAL,
                        FindingCategory.SANDBOX_ESCAPE,
                        "PY-AST-012",
                        "getattr + __import__ 混淆导入（沙箱逃逸手法）",
                        line.trim(),
                        i + 1,
                        "混淆导入是沙箱逃逸的典型手法"
                ));
            }
        }
    }

    /**
     * 分析字符串拼接混淆
     */
    private void analyzeStringConcat(String content, List<SecurityFinding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher m = CONCAT_ASSIGN_PATTERN.matcher(line);
            if (m.find()) {
                findings.add(SecurityFinding.of(
                        Severity.LOW,
                        FindingCategory.OBFUSCATION,
                        "PY-AST-020",
                        "变量赋值使用了字符串拼接（可能是混淆技术）",
                        m.group().trim(),
                        i + 1,
                        "字符串拼接可能是为了绕过字符串匹配检测"
                ));
            }
        }
    }

    @Override
    public String supportedType() {
        return "python";
    }

}
