package com.hxh.apboa.security.script;

import com.hxh.apboa.security.script.checker.HtmlSecurityChecker;
import com.hxh.apboa.security.script.checker.NodeJsSecurityChecker;
import com.hxh.apboa.security.script.checker.PythonSecurityChecker;
import com.hxh.apboa.security.script.checker.ShellSecurityChecker;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.SecurityFinding;
import com.hxh.apboa.security.script.model.SecurityReport;

import java.util.*;

/**
 * 描述：脚本安全检查服务 — 项目的唯一对外入口。
 * 自动根据文件名识别脚本类型并路由到对应的检查器执行安全检查。
 * 当无法识别文件类型或找不到对应检查器时，返回安全的报告（而非抛出异常）。
 *
 * <pre>{@code
 *   // 使用示例
 *   ScriptSecurityService service = new ScriptSecurityService();
 *   SecurityReport report = service.check("deploy.sh", shellContent);
 *   System.out.println("安全: " + report.safe());
 * }</pre>
 *
 * @author huxuehao
 */
public class ScriptSecurityService {

    /** 检查器注册中心 */
    private final CheckerRegistry registry;

    /**
     * 构造服务并加载所有内置检查器
     */
    public ScriptSecurityService() {
        this.registry = new CheckerRegistry();
        loadBuiltinCheckers();
    }

    /**
     * 使用自定义的注册中心构造服务
     *
     * @param registry 自定义检查器注册中心
     */
    public ScriptSecurityService(CheckerRegistry registry) {
        this.registry = registry;
    }

    /**
     * 加载所有内置的默认检查器
     */
    private void loadBuiltinCheckers() {
        registry.register(new ShellSecurityChecker());
        registry.register(new PythonSecurityChecker());
        registry.register(new NodeJsSecurityChecker());
        registry.register(new HtmlSecurityChecker());
    }

    /**
     * 根据文件名自动识别类型并执行安全检查
     * 当无法识别文件类型时，返回安全的报告（无任何发现）
     *
     * @param fileName 文件名（如 "install.sh"）
     * @param content  脚本文件内容
     * @return 安全检查报告（无法识别类型时返回安全的空报告）
     */
    public SecurityReport check(String fileName, String content) {
        Optional<ScriptType> optionalType = ScriptType.fromFileName(fileName);

        // 无法识别文件类型，返回安全的空报告
        return optionalType.map(scriptType -> check(scriptType, fileName, content)).orElseGet(() -> SecurityReport.of(fileName, null, List.of()));

    }

    /**
     * 明确指定类型执行安全检查
     * 当找不到对应类型的检查器时，返回安全的报告（无任何发现）
     *
     * @param type    脚本类型
     * @param content 脚本内容
     * @return 安全检查报告（找不到检查器时返回安全的空报告）
     */
    public SecurityReport check(ScriptType type, String content) {
        return check(type, null, content);
    }

    /**
     * 按类型和文件名执行检查
     * 当找不到对应类型的检查器时，返回安全的报告（无任何发现）
     */
    private SecurityReport check(ScriptType type, String fileName, String content) {
        Optional<ScriptSecurityChecker> optionalChecker = registry.get(type);

        if (optionalChecker.isEmpty()) {
            // 未找到对应类型的检查器，返回安全的空报告
            return SecurityReport.of(fileName, type, List.of());
        }

        ScriptSecurityChecker checker = optionalChecker.get();
        List<SecurityFinding> findings = checker.check(content);
        return SecurityReport.of(fileName, type, findings);
    }

    /**
     * 批量检查多个文件（自动按文件名识别类型）
     * 无法识别的文件类型或找不到检查器时会返回安全的空报告
     *
     * @param files Map<文件名, 文件内容>
     * @return 每个文件的检查报告列表
     */
    public List<SecurityReport> checkAll(Map<String, String> files) {
        List<SecurityReport> reports = new ArrayList<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            reports.add(check(entry.getKey(), entry.getValue()));
        }
        return reports;
    }

    /**
     * 获取检查器注册中心（允许用户动态注册/覆盖检查器）
     *
     * @return 检查器注册中心
     */
    public CheckerRegistry getRegistry() {
        return registry;
    }

}
