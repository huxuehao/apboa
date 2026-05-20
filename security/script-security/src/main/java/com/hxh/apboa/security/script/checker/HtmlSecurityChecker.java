package com.hxh.apboa.security.script.checker;

import com.hxh.apboa.security.script.AbstractScriptChecker;
import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.RuleDefinition;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.Severity;

import java.util.regex.Pattern;

/**
 * 描述：HTML 文件安全检查器，覆盖 XSS跨站脚本、外部资源加载、iframe嵌入、事件处理器注入、
 * 伪协议利用、HTML导入、SVG内嵌脚本、Meta刷新重定向、Form外泄、模板注入等攻击面。
 *
 * @author huxuehao
 */
public class HtmlSecurityChecker extends AbstractScriptChecker {

    public HtmlSecurityChecker() {
        registerScriptTagRulesLoose();
        registerIframeRulesLoose();
        registerEventHandlerRulesLoose();
        registerPseudoProtocolRules();
        registerExternalResourceRulesLoose();
        registerMetaRefreshRulesLoose();
        registerFormRules();
        registerPluginRules();
        registerSvgRules();
        registerTemplateInjectionRules();
        registerMultiLineRulesLoose();
    }

    @Override
    public ScriptType supportedType() {
        return ScriptType.HTML;
    }

    // ======================== Script标签（宽松） ========================

    private void registerScriptTagRulesLoose() {
        // 外部脚本：仅当非HTTPS或可疑域名时才告警
        registerRule(RuleDefinition.of("HTML-001", Severity.LOW, FindingCategory.XSS,
                "<script[^>]*src\\s*=\\s*['\"]http://[^\"']+['\"]",
                "Script标签加载HTTP外部脚本", "HTTP外部脚本存在中间人攻击风险，建议使用HTTPS"));
        // 危险JS操作：保持HIGH（eval仍应避免）
        registerRule(RuleDefinition.of("HTML-003", Severity.MEDIUM, FindingCategory.CODE_EXECUTION,
                "<script[^>]*>[\\s\\S]{0,200}(eval\\s*\\(|Function\\s*\\(|document\\.write\\s*\\()",
                "Script标签内包含危险JS操作", "eval/Function存在代码执行风险，游戏场景尽量用其他方式"));
    }

    // ======================== Iframe标签（宽松） ========================

    private void registerIframeRulesLoose() {
        // 外部iframe：仅警告HTTP或可疑域名
        registerRule(RuleDefinition.of("HTML-010", Severity.LOW, FindingCategory.XSS,
                "<iframe[^>]*src\\s*=\\s*['\"]http://[^\"']+['\"]",
                "Iframe嵌入HTTP外部页面", "HTTP外部iframe存在安全风险，建议使用HTTPS"));

        // srcdoc包含script：保持MEDIUM
        registerRule(RuleDefinition.of("HTML-011", Severity.MEDIUM, FindingCategory.XSS,
                "<iframe[^>]*srcdoc\\s*=\\s*['\"][^\"']*<script", "Iframe srcdoc中包含script",
                "srcdoc中的脚本可直接执行，请确认内容可信"));
    }

    // ======================== 事件处理器（宽松） ========================

    private void registerEventHandlerRulesLoose() {
        // 只有包含危险函数的才告警
        registerRule(RuleDefinition.of("HTML-020", Severity.MEDIUM, FindingCategory.XSS,
                "on\\w+\\s*=\\s*['\"][^\"']*(eval|Function|alert|prompt|confirm)\\s*\\(",
                "事件处理器中包含危险函数调用", "事件处理器中的危险函数可能存在XSS风险"));
    }

    // ======================== 伪协议（保持严格） ========================

    private void registerPseudoProtocolRules() {
        // javascript
        registerRule(RuleDefinition.of("HTML-030", Severity.CRITICAL, FindingCategory.XSS,
                "(href|src|action|formaction)\\s*=\\s*['\"]javascript\\s*:", "javascript: 伪协议",
                "javascript:伪协议可以直接执行恶意JS代码，应使用addEventListener替代"));

        // vbscript
        registerRule(RuleDefinition.of("HTML-031", Severity.CRITICAL, FindingCategory.XSS,
                "(href|src|action)\\s*=\\s*['\"]vbscript\\s*:", "vbscript: 伪协议",
                "vbscript:伪协议（IE遗留）可以执行恶意脚本"));

        // data:text/html
        registerRule(RuleDefinition.of("HTML-032", Severity.CRITICAL, FindingCategory.XSS,
                "(href|src)\\s*=\\s*['\"]data\\s*:\\s*text/html", "data:text/html 数据URI",
                "data URI中的HTML可能包含恶意脚本"));

        // Base64 data
        registerRule(RuleDefinition.of("HTML-033", Severity.LOW, FindingCategory.XSS,
                "data\\s*:\\s*[^\"'\\s>]*base64[^\"'\\s>]*", "Base64编码的data URI",
                "Base64 data URI可能隐藏恶意内容，请快速确认解码后内容"));
    }

    // ======================== 外部资源（宽松） ========================

    private void registerExternalResourceRulesLoose() {
        // 外部CSS：仅HTTP告警
        registerRule(RuleDefinition.of("HTML-040", Severity.LOW, FindingCategory.XSS,
                "<link[^>]*href\\s*=\\s*['\"]http://[^\"']+['\"][^>]*rel\\s*=\\s*['\"]stylesheet",
                "加载HTTP外部CSS", "HTTP外部CSS可能被篡改，建议使用HTTPS"));

        // Base标签
        registerRule(RuleDefinition.of("HTML-044", Severity.HIGH, FindingCategory.XSS,
                "<base\\s+[^>]*href\\s*=\\s*['\"]https?://", "Base标签修改基础URL",
                "Base标签可能劫持页面中的相对路径，请确认无误"));
    }

    // ======================== Meta刷新（宽松） ========================

    private void registerMetaRefreshRulesLoose() {
        // 只告警重定向到非信任域
        registerRule(RuleDefinition.of("HTML-050", Severity.LOW, FindingCategory.XSS,
                "<meta[^>]*http-equiv\\s*=\\s*['\"]refresh['\"][^>]*content\\s*=\\s*['\"]\\d+\\s*;\\s*url\\s*=\\s*['\"]http://[^\"']+",
                "Meta标签HTTP刷新重定向到HTTP站点", "重定向到HTTP站点存在安全风险"));
    }

    // ======================== Form表单（保持） ========================

    private void registerFormRules() {
        // 仅POST到外部HTTP域名告警
        registerRule(RuleDefinition.of("HTML-060", Severity.MEDIUM, FindingCategory.DATA_EXFILTRATION,
                "<form[^>]*action\\s*=\\s*['\"]http://[^\"']+['\"][^>]*method\\s*=\\s*['\"]post['\"]",
                "Form表单POST到HTTP外部URL", "表单数据通过HTTP传输存在泄露风险"));

        // 敏感隐藏字段告警
        registerRule(RuleDefinition.of("HTML-061", Severity.MEDIUM, FindingCategory.DATA_EXFILTRATION,
                "<input[^>]*type\\s*=\\s*['\"]hidden['\"][^>]*name\\s*=\\s*['\"](pass|token|secret|key)",
                "隐藏input包含敏感字段名", "敏感数据可能通过隐藏表单字段传递"));
    }

    // ======================== SVG（宽松） ========================

    private void registerSvgRules() {
        // SVG script
        registerRule(RuleDefinition.of("HTML-080", Severity.HIGH, FindingCategory.XSS,
                "<svg[^>]*>[\\s\\S]{0,300}<script", "SVG内嵌script标签",
                "SVG中的script可以执行JS，游戏场景如需使用请确保来源可信"));

        // SVG事件
        registerRule(RuleDefinition.of("HTML-081", Severity.MEDIUM, FindingCategory.XSS,
                "<svg[^>]*on(load|click|mouseover|error)\\s*=", "SVG内嵌事件处理器",
                "SVG中的事件处理器可执行JS，游戏场景如需使用请确保内容可控"));
    }

    // ======================== 模板注入（保持严格） ========================

    private void registerTemplateInjectionRules() {
        // 模板注入RCE
        registerRule(RuleDefinition.of("HTML-090", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "\\{\\{[^}]*(constructor|__proto__|process|global|require|child_process)[^}]*\\}\\}",
                "模板语法中访问危险对象", "模板注入可导致任意代码执行"));

        // v-html
        registerRule(RuleDefinition.of("HTML-092", Severity.MEDIUM, FindingCategory.XSS,
                "v-html\\s*=\\s*['\"][^\"']+['\"]", "Vue v-html指令",
                "v-html会直接渲染HTML，确保内容已转义"));
    }

    // ======================== 多行规则（宽松） ========================

    private void registerMultiLineRulesLoose() {
        // 钓鱼表单：CRITICAL
        registerMultiLineRule(new RuleDefinition("HTML-M002", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                Pattern.compile("<form[^>]*action\\s*=\\s*['\"]https?://[^\"']+['\"][\\s\\S]{0,500}<input[^>]*type\\s*=\\s*['\"]password",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
                "Form POST到外部 + 密码输入框 钓鱼表单", "疑似钓鱼表单，密码将被发送到外部服务器"));

        // SVG完整script：HIGH
        registerMultiLineRule(new RuleDefinition("HTML-M003", Severity.HIGH, FindingCategory.XSS,
                Pattern.compile("<svg[^>]*>[\\s\\S]{0,2000}<script[^>]*>[\\s\\S]{0,500}</script>",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
                "SVG内嵌完整Script块", "SVG中的Script标签可以执行JavaScript"));
    }

    // ======================== 插件/Object（保持严格） ========================

    private void registerPluginRules() {
        registerRule(RuleDefinition.of("HTML-070", Severity.HIGH, FindingCategory.XSS,
                "<object[^>]*data\\s*=\\s*['\"]https?://", "Object标签加载外部资源",
                "Object可能加载Flash/ActiveX，现代浏览器已限制"));

        registerRule(RuleDefinition.of("HTML-072", Severity.CRITICAL, FindingCategory.XSS,
                "<applet[^>]*>", "Applet标签", "Java Applet可导致RCE，应完全避免"));
    }
}
