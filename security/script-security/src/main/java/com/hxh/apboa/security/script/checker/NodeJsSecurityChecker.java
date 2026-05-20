package com.hxh.apboa.security.script.checker;

import com.hxh.apboa.security.script.AbstractScriptChecker;
import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.RuleDefinition;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.Severity;

import java.util.regex.Pattern;

/**
 * 描述：Node.js/JavaScript 脚本安全检查器，覆盖命令执行（child_process）、代码执行（eval/Function）、
 * 文件破坏（fs模块）、反弹Shell（net模块）、沙箱逃逸（vm模块）、原型污染、数据外泄、
 * WebAssembly原生代码、Worker线程等攻击面。
 *
 * @author huxuehao
 */
public class NodeJsSecurityChecker extends AbstractScriptChecker {

    public NodeJsSecurityChecker() {
        registerChildProcessRules();
        registerCodeExecutionRules();
        registerFileSystemRules();
        registerReverseShellRules();
        registerSandboxEscapeRules();
        registerPrototypePollutionRules();
        registerDataExfiltrationRules();
        registerNativeCodeRules();
        registerProcessRules();
        registerObfuscationRules();
        registerInformationGatheringRules();
        registerMultiLineRules();
    }

    @Override
    public ScriptType supportedType() {
        return ScriptType.NODEJS;
    }

    // ======================== child_process 命令执行 ========================

    private void registerChildProcessRules() {
        registerRule(RuleDefinition.of("JS-001", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "require\\s*\\(\\s*['\"]child_process['\"]\\s*\\)",
                "require('child_process')导入进程模块", "child_process可用于执行任意系统命令"));
        registerRule(RuleDefinition.of("JS-002", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "import\\s+.*from\\s+['\"]child_process['\"]",
                "import child_process模块", "child_process可用于执行任意系统命令"));
        registerRule(RuleDefinition.of("JS-003", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "\\.(exec|execSync)\\s*\\(",
                "child_process.exec/execSync执行命令", "exec/execSync可执行任意shell命令"));
        registerRule(RuleDefinition.of("JS-004", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "\\.(spawn|spawnSync)\\s*\\(",
                "child_process.spawn/spawnSync执行命令", "spawn可执行任意系统命令"));
        registerRule(RuleDefinition.of("JS-005", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "\\.(fork|execFile|execFileSync)\\s*\\(",
                "child_process.fork/execFile执行命令", "fork/execFile可执行任意系统命令"));
        registerRule(RuleDefinition.of("JS-006", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "(exec|spawn|fork|execFile)\\s*\\(\\s*['\"](sh|bash|/bin/sh|/bin/bash|cmd\\.exe)",
                "直接执行Shell解释器", "直接调用Shell解释器执行命令极度危险"));
        registerRule(RuleDefinition.of("JS-007", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "\\{.*shell\\s*:\\s*true", "命令选项中启用shell模式", "shell:true存在命令注入风险"));
    }

    // ======================== 代码执行 ========================

    private void registerCodeExecutionRules() {
        registerRule(RuleDefinition.of("JS-015", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "eval\\s*\\(", "eval()执行字符串代码", "eval()执行不可信字符串可能导致任意代码执行"));
        registerRule(RuleDefinition.of("JS-016", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "new\\s+Function\\s*\\(", "new Function()动态构造函数", "Function构造器可执行任意JS代码"));
        registerRule(RuleDefinition.of("JS-017", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "Function\\s*\\(", "Function()直接调用（等同于new Function）",
                "Function()可执行任意JavaScript代码"));
        registerRule(RuleDefinition.of("JS-018", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "setTimeout\\s*\\(\\s*['\"][^'\"]+['\"]", "setTimeout传入字符串代码",
                "setTimeout的字符串参数会被eval执行"));
        registerRule(RuleDefinition.of("JS-019", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "setInterval\\s*\\(\\s*['\"][^'\"]+['\"]", "setInterval传入字符串代码",
                "setInterval的字符串参数会被eval执行"));
        registerRule(RuleDefinition.of("JS-020", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "import\\s*\\(\\s*\\w+\\s*\\+", "动态import拼接路径", "动态import拼接可能存在路径遍历风险"));
    }

    // ======================== 文件系统操作 ========================

    private void registerFileSystemRules() {
        registerRule(RuleDefinition.of("JS-025", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "\\.rmSync\\s*\\(\\s*['\"]/['\"]",
                "fs.rmSync('/')删除根目录", "删除根目录是灾难性操作"));
        registerRule(RuleDefinition.of("JS-026", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "\\.(rmSync|rmdirSync|unlinkSync)\\s*\\(\\s*['\"]/(etc|var|usr|boot|home)",
                "fs删除系统目录", "删除系统目录会导致系统不可用"));
        registerRule(RuleDefinition.of("JS-027", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "\\.writeFileSync\\s*\\(\\s*['\"]/(etc|bin|sbin)/",
                "fs.writeFileSync写入系统文件", "写入系统文件可能损坏系统配置"));
        registerRule(RuleDefinition.of("JS-028", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "\\.appendFileSync\\s*\\(\\s*['\"]/(etc|bin|sbin)/",
                "fs.appendFileSync追加写入系统文件", "追加写入系统文件可能被用于持久化"));
        registerRule(RuleDefinition.of("JS-029", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "\\.chmodSync\\s*\\(\\s*['\"]/(etc|bin|sbin)/[^'\"]+['\"]",
                "fs.chmodSync修改系统文件权限", "修改系统文件权限可能打开安全漏洞"));
        registerRule(RuleDefinition.of("JS-030", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "\\.chownSync\\s*\\(\\s*['\"]/(etc|bin|sbin)/",
                "fs.chownSync修改系统文件所有者", "修改系统文件所有者可能导致安全问题"));
        registerRule(RuleDefinition.of("JS-031", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "\\.truncateSync\\s*\\(\\s*['\"]/(etc|bin|sbin)/",
                "fs.truncateSync截断系统文件", "截断系统文件可能导致配置丢失"));
        registerRule(RuleDefinition.of("JS-032", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.readFileSync\\s*\\(\\s*['\"]/(etc/passwd|etc/shadow)['\"]",
                "fs.readFileSync读取密码文件", "密码文件包含敏感账户信息"));
    }

    // ======================== 反弹Shell ========================

    private void registerReverseShellRules() {
        registerRule(RuleDefinition.of("JS-040", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "net\\.(Socket|createConnection|connect)\\s*\\([^)]*\\)[\\s\\S]{0,200}\\.pipe\\s*\\(",
                "Net模块创建连接并pipe（反弹Shell）", "net模块 + pipe是典型的Node.js反弹Shell模式"));
        registerRule(RuleDefinition.of("JS-041", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "require\\s*\\(\\s*['\"]net['\"]\\s*\\)[\\s\\S]{0,300}\\.(spawn|exec)",
                "Net模块 + child_process 反弹Shell组合", "net和child_process组合是反弹Shell特征"));
        registerRule(RuleDefinition.of("JS-042", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "(sh|bash|cmd|powershell)\\s*\\.(stdin|stdout|stderr)\\s*\\.pipe\\s*\\(",
                "Shell进程IO重定向（反弹Shell）", "Shell IO pipe重定向是反弹Shell核心手法"));
        registerRule(RuleDefinition.of("JS-043", Severity.HIGH, FindingCategory.REVERSE_SHELL,
                "require\\s*\\(\\s*['\"]net['\"]\\s*\\)[\\s\\S]{0,300}(?:connect|spawn|exec|listen|createConnection)",
                "Net模块创建服务器监听", "未授权的端口监听可能是后门"));
        registerRule(RuleDefinition.of("JS-044", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "require\\s*\\(\\s*['\"]dgram['\"]\\s*\\)", "dgram UDP模块", "UDP通信可能被用于隐蔽信道"));
        registerRule(RuleDefinition.of("JS-045", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "require\\s*\\(\\s*['\"]tls['\"]\\s*\\)", "TLS加密模块", "TLS可能被用于加密恶意通信"));
    }

    // ======================== 沙箱逃逸（vm模块） ========================

    private void registerSandboxEscapeRules() {
        registerRule(RuleDefinition.of("JS-050", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "vm\\.(runInNewContext|runInThisContext|compileFunction|Script)\\s*\\(",
                "vm模块执行代码", "vm模块的沙箱可以被逃逸"));
        registerRule(RuleDefinition.of("JS-051", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "vm2\\s*", "vm2沙箱库（已知多个逃逸漏洞）", "vm2存在多个已知沙箱逃逸漏洞"));
        registerRule(RuleDefinition.of("JS-052", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "process\\.binding\\s*\\(", "process.binding()访问Node.js内部C++绑定",
                "process.binding是vm沙箱逃逸的常见方式"));
        registerRule(RuleDefinition.of("JS-053", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "process\\._linkedBinding\\s*\\(", "process._linkedBinding()访问隐藏绑定",
                "_linkedBinding是沙箱逃逸手法"));
        registerRule(RuleDefinition.of("JS-054", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "process\\.dlopen\\s*\\(", "process.dlopen()加载原生模块", "dlopen可加载任意.so/.dll库"));
        registerRule(RuleDefinition.of("JS-055", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "require\\s*\\(\\s*['\"]module['\"]\\s*\\)\\.(createRequire|_load)\\s*\\(",
                "module.createRequire/_load动态加载", "动态require加载可绕过模块限制"));
        registerRule(RuleDefinition.of("JS-056", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "require\\s*\\(\\s*['\"]repl['\"]\\s*\\)\\.start\\s*\\(",
                "repl.start()创建交互式环境", "REPL可被用于交互式代码执行"));
        registerRule(RuleDefinition.of("JS-057", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "require\\s*\\(\\s*['\"]inspector['\"]\\s*\\)\\.open\\s*\\(",
                "inspector.open()打开调试器", "打开inspector可允许远程调试和代码执行"));
    }

    // ======================== 原型污染 ========================

    private void registerPrototypePollutionRules() {
        registerRule(RuleDefinition.of("JS-065", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "__proto__\\s*\\[\\s*['\"](?:constructor|__proto__|prototype)?['\"]\\s*\\]\\s*=", "设置__proto__属性（原型污染）",
                "原型污染可能导致远程代码执行"));
        registerRule(RuleDefinition.of("JS-066", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "(?:constructor\\s*\\.\\s*prototype|prototype\\s*\\[|\\w+\\[\\s*['\"]__proto__['\"])\\s*\\[\\s*['\"]\\w+['\"]\\s*\\]\\s*=",
                "修改constructor.prototype（原型污染）", "原型污染可能导致远程代码执行"));
        registerRule(RuleDefinition.of("JS-067", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "\\w+\\[\\s*['\"]__proto__['\"]\\s*\\]\\s*\\[\\s*['\"]\\w+['\"]\\s*\\]\\s*=",
                "通过属性访问设置__proto__（原型污染）", "原型污染可能导致远程代码执行"));
        registerRule(RuleDefinition.of("JS-068", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "Object\\.assign\\s*\\(.*__proto__", "Object.assign修改__proto__",
                "通过Object.assign进行原型污染"));
        registerRule(RuleDefinition.of("JS-069", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "\\w+\\s*\\[\\s*['\"]constructor['\"]\\s*\\]\\s*\\[\\s*['\"]prototype['\"]\\s*\\]",
                "访问constructor.prototype链", "constructor.prototype遍历可能是原型污染尝试"));
    }

    // ======================== 数据外泄 ========================

    private void registerDataExfiltrationRules() {
        registerRule(RuleDefinition.of("JS-075", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "(http|https)\\.request\\s*\\([^)]*\\)[\\s\\S]{0,200}(readFileSync|createReadStream)",
                "HTTP请求 + 文件读取 数据外泄", "疑似将读取的文件内容通过HTTP外传"));
        registerRule(RuleDefinition.of("JS-076", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "fetch\\s*\\([^)]*(?:readFileSync|readFile)[^)]*\\)",
                "fetch + 文件读取 数据外泄", "疑似将读取的文件内容通过fetch外传"));
        registerRule(RuleDefinition.of("JS-077", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "WebSocket\\s*\\([^)]+\\)[\\s\\S]{0,200}(readFileSync|createReadStream)",
                "WebSocket + 文件读取 数据外泄", "WebSocket可能被用于持续外泄数据"));
        registerRule(RuleDefinition.of("JS-078", Severity.MEDIUM, FindingCategory.DATA_EXFILTRATION,
                "zlib\\.(deflate|gzip)\\s*\\([^)]*\\)[\\s\\S]{0,200}\\.(request|fetch)",
                "压缩数据后外传", "压缩可能是数据外泄前准备"));
        registerRule(RuleDefinition.of("JS-079", Severity.MEDIUM, FindingCategory.DATA_EXFILTRATION,
                "crypto\\.(createCipher|createCipheriv)\\s*\\([^)]*\\)[\\s\\S]{0,200}\\.(request|fetch)",
                "加密数据后外传", "加密可能是数据外泄前准备"));
        registerRule(RuleDefinition.of("JS-080", Severity.MEDIUM, FindingCategory.INFORMATION_GATHERING,
                "os\\.(networkInterfaces|cpus|userInfo|homedir|hostname)\\s*\\(",
                "os模块收集系统信息", "系统信息收集可能是攻击前置步骤"));
    }

    // ======================== 原生代码执行 ========================

    private void registerNativeCodeRules() {
        registerRule(RuleDefinition.of("JS-085", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "WebAssembly\\.(instantiate|compile)\\s*\\(",
                "WebAssembly执行原生代码", "WebAssembly可执行编译后的任意原生代码"));
        registerRule(RuleDefinition.of("JS-086", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "WebAssembly\\.instantiateStreaming\\s*\\(",
                "WebAssembly流式加载执行", "WebAssembly流式加载执行原生代码"));
        registerRule(RuleDefinition.of("JS-087", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "require\\s*\\(\\s*['\"]ffi['\"]\\s*\\)|require\\s*\\(\\s*['\"]ffi-napi['\"]\\s*\\)",
                "FFI外部函数接口库", "FFI可直接调用任意C函数"));
        registerRule(RuleDefinition.of("JS-088", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "require\\s*\\(\\s*['\"]ref['\"]\\s*\\)", "Ref指针操作库", "ref库配合FFI用于原生内存操作"));
        registerRule(RuleDefinition.of("JS-089", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "Worker\\s*\\([^)]*\\)[\\s\\S]{0,100}(exec|spawn|eval|Function)",
                "Worker线程中执行危险操作", "Worker线程可能用于隐藏恶意代码"));
    }

    // ======================== 进程操作 ========================

    private void registerProcessRules() {
        registerRule(RuleDefinition.of("JS-095", Severity.HIGH, FindingCategory.SERVICE_MANIPULATION,
                "process\\.exit\\s*\\(", "process.exit()终止进程", "检查进程退出是否必要"));
        registerRule(RuleDefinition.of("JS-096", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "process\\.(setuid|setgid|seteuid|setegid)\\s*\\(",
                "process.setuid/setgid修改进程身份", "修改进程身份可能用于权限提升"));
        registerRule(RuleDefinition.of("JS-097", Severity.HIGH, FindingCategory.SERVICE_MANIPULATION,
                "process\\.kill\\s*\\(", "process.kill发送信号", "检查目标进程是否关键系统进程"));
        registerRule(RuleDefinition.of("JS-098", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "process\\.env", "访问process.env环境变量", "环境变量可能包含密钥等敏感信息"));
        registerRule(RuleDefinition.of("JS-099", Severity.HIGH, FindingCategory.PERSISTENCE,
                "require\\s*\\(\\s*['\"]cluster['\"]\\s*\\)\\.(fork|setupMaster)\\s*\\(",
                "cluster模块创建子进程（持久化）", "多个子进程可能用于持久化或资源消耗"));
    }

    // ======================== 代码混淆 ========================

    private void registerObfuscationRules() {
        registerRule(RuleDefinition.of("JS-105", Severity.HIGH, FindingCategory.OBFUSCATION,
                "Buffer\\.from\\s*\\([^)]+,\\s*['\"]base64['\"]\\s*\\)[\\s\\S]{0,100}(eval|Function|exec)",
                "Base64解码后执行", "Base64解码后执行是混淆恶意代码的常见模式"));
        registerRule(RuleDefinition.of("JS-106", Severity.HIGH, FindingCategory.OBFUSCATION,
                "atob\\s*\\([^)]+\\)[\\s\\S]{0,100}(eval|Function|exec)",
                "atob解码后执行", "Base64解码后执行是混淆恶意代码的常见模式"));
        registerRule(RuleDefinition.of("JS-107", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "fromCharCode\\s*\\([^)]+\\.reduce", "String.fromCharCode拼接", "fromCharCode可能用于混淆字符串"));
        registerRule(RuleDefinition.of("JS-108", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "unescape\\s*\\(.*eval", "unescape解码后eval", "URL解码后eval执行"));
    }

    // ======================== 信息收集 ========================

    private void registerInformationGatheringRules() {
        registerRule(RuleDefinition.of("JS-100", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/etc/(hosts|resolv\\.conf|fstab)['\"]",
                "fs模块读取系统配置文件", "系统配置文件包含网络和挂载信息"));
        registerRule(RuleDefinition.of("JS-101", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/proc/(cpuinfo|meminfo|version|modules)['\"]",
                "fs模块读取/proc系统信息", "内核信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("JS-102", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "os\\.(arch|platform|type|release|version|machine|endianness)\\s*\\(",
                "os模块获取系统信息", "系统信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("JS-103", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](ifconfig|ip\\s+(addr|link))",
                "child_process执行网络接口命令", "网络拓扑信息收集"));

        // 防火墙规则探测
        registerRule(RuleDefinition.of("JS-104", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](iptables|nft|ufw)",
                "child_process执行防火墙查询命令", "防火墙规则暴露网络防御策略，禁止AI探测"));

        // 进程列表探测
        registerRule(RuleDefinition.of("JS-105", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](ps|pgrep|pidof)",
                "child_process执行进程查询命令", "进程列表暴露运行中的服务和安全软件"));

        // 网络连接探测
        registerRule(RuleDefinition.of("JS-106", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](netstat|ss|lsof)",
                "child_process执行网络连接查询命令", "网络连接信息暴露通信拓扑和服务端口"));

        // 环境变量和敏感配置
        registerRule(RuleDefinition.of("JS-107", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"].*\\.env['\"]|dotenv\\.config|require\\s*\\(\\s*['\"]dotenv['\"]\\s*\\)",
                "读取.env环境变量文件", ".env文件通常包含密钥、Token等敏感凭证"));
        registerRule(RuleDefinition.of("JS-108", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"].*/(\\.ssh|\\.aws|\\.kube|\\.docker|\\.config)/",
                "fs模块读取SSH/AWS/K8s/Docker密钥目录", "读取密钥和凭证文件是明确的信息窃取行为"));
        registerRule(RuleDefinition.of("JS-109", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/etc/(sudoers|security/|selinux/|apparmor/)['\"]",
                "fs模块读取安全配置文件", "安全配置文件暴露系统防御机制"));

        // 系统日志探测
        registerRule(RuleDefinition.of("JS-110", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/var/log/",
                "fs模块读取系统日志文件", "系统日志可能包含敏感操作记录和凭证信息"));

        // 内核模块探测
        registerRule(RuleDefinition.of("JS-111", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](lsmod|systemctl|service)",
                "child_process执行内核模块/服务查询命令", "内核模块列表和服务状态可用于检测安全监控"));

        // 定时任务探测
        registerRule(RuleDefinition.of("JS-112", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"]crontab|\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/etc/cron",
                "查看定时任务", "定时任务可能暴露系统自动化流程和脚本路径"));

        // 历史命令探测
        registerRule(RuleDefinition.of("JS-113", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"].*/(\\.bash_history|\\.zsh_history|\\.mysql_history|\\.psql_history|\\.python_history|\\.node_repl_history)",
                "fs模块读取Shell历史记录", "历史记录可能包含密码、密钥等敏感操作"));

        // 磁盘挂载信息
        registerRule(RuleDefinition.of("JS-114", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](df|lsblk)|\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/proc/mounts",
                "查看磁盘挂载信息", "磁盘挂载信息暴露存储拓扑"));

        // 用户和组信息
        registerRule(RuleDefinition.of("JS-115", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readFileSync|createReadStream)\\s*\\(\\s*['\"]/etc/group['\"]|(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](getent|whoami|id|who|last)",
                "查看用户和组信息", "用户列表暴露系统账户信息"));

        // 容器环境探测
        registerRule(RuleDefinition.of("JS-116", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](docker|kubectl|podman|crictl)",
                "child_process执行容器命令", "容器环境信息暴露基础设施架构"));

        // 敏感文件搜索
        registerRule(RuleDefinition.of("JS-117", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](find|locate|which|whereis)",
                "child_process执行文件搜索命令", "搜索文件可能用于定位密钥和敏感配置"));
        registerRule(RuleDefinition.of("JS-118", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "glob\\.(sync|glob)\\s*\\(.*\\.(env|ini|conf|config|yml|yaml|json|properties|toml)",
                "glob搜索配置文件", "批量搜索配置文件可能用于信息收集"));
        registerRule(RuleDefinition.of("JS-119", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "glob\\.(sync|glob)\\s*\\(.*\\.(pem|key|crt|cer|pfx|p12|jks|keystore)",
                "glob搜索密钥和证书文件", "搜索密钥文件是明确的凭证窃取行为"));

        // 内核参数探测
        registerRule(RuleDefinition.of("JS-120", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(exec|spawn|execSync|spawnSync)\\s*\\(\\s*['\"](sysctl|env|printenv|set\\b)",
                "child_process执行内核参数/环境变量命令", "内核参数和环境变量暴露系统配置"));

        // fs模块遍历系统目录
        registerRule(RuleDefinition.of("JS-121", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "\\.(readdirSync|opendirSync|statSync)\\s*\\(\\s*['\"]/(etc|root|home)/",
                "fs模块遍历系统目录", "遍历系统目录可能用于信息收集"));

        // 进程运行时配置信息（补充process.env之外的）
        registerRule(RuleDefinition.of("JS-122", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "process\\.(config|versions|features|execPath|execArgv|argv|pid|ppid|title)\\b",
                "process模块获取运行时配置", "运行时配置信息收集"));
    }

    // ======================== 多行规则 ========================

    private void registerMultiLineRules() {
        registerMultiLineRule(new RuleDefinition("JS-M001", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                Pattern.compile("require\\s*\\(\\s*['\"](net|child_process)['\"]\\s*\\)[\\s\\S]{0,400}(spawn|exec|pipe|connect)",
                        Pattern.DOTALL),
                "Net + ChildProcess 反弹Shell组合", "典型的Node.js反弹Shell实现"));

        registerMultiLineRule(new RuleDefinition("JS-M002", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                Pattern.compile("vm\\.(runInNewContext|runInThisContext)[\\s\\S]{0,500}(process\\.binding|constructor\\.constructor)",
                        Pattern.DOTALL),
                "VM模块 + 沙箱逃逸链", "vm沙箱中的逃逸尝试"));

        registerMultiLineRule(new RuleDefinition("JS-M003", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                Pattern.compile("(readFileSync|createReadStream)[\\s\\S]{0,400}(http\\.request|fetch|WebSocket)",
                        Pattern.DOTALL),
                "文件读取 + 网络请求 数据外泄链", "读取文件并外发的组合模式"));

        registerMultiLineRule(new RuleDefinition("JS-M004", Severity.CRITICAL, FindingCategory.OBFUSCATION,
                Pattern.compile("(Buffer\\.from|atob)[\\s\\S]{0,200}(eval|Function|vm\\.)",
                        Pattern.DOTALL),
                "解码 + 执行 混淆代码链", "解码后立即执行的混淆代码模式"));

        // 敏感文件读取 + 网络外传 信息窃取链
        registerMultiLineRule(new RuleDefinition("JS-M005", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("(readFileSync|createReadStream)[\\s\\S]{0,300}(http\\.request|fetch|WebSocket|net\\.connect)",
                        Pattern.DOTALL),
                "读取文件后通过网络外传", "典型的Node.js敏感信息窃取链"));

        // 系统信息全面收集链
        registerMultiLineRule(new RuleDefinition("JS-M006", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("(exec|spawn|execSync)\\s*\\(\\s*['\"](ps|netstat|env|printenv|uname)[\\s\\S]{0,300}(process\\.env|os\\.(networkInterfaces|cpus|userInfo|homedir))",
                        Pattern.DOTALL),
                "批量收集系统信息", "系统信息全面收集是攻击前置侦察行为"));
    }

}
