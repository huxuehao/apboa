package com.hxh.apboa.security.script.checker;

import com.hxh.apboa.security.script.AbstractScriptChecker;
import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.RuleDefinition;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.Severity;

import java.util.regex.Pattern;

/**
 * 描述：Python 脚本安全检查器，覆盖命令执行、代码执行、反序列化攻击、沙箱逃逸、
 * 文件破坏、权限提升、数据外泄、网络攻击、代码混淆等攻击面。
 * 正则层覆盖常见模式，AST深度分析由 PythonAstAnalyzer 负责。
 *
 * @author huxuehao
 */
public class PythonSecurityChecker extends AbstractScriptChecker {

    public PythonSecurityChecker() {
        registerCommandExecutionRules();
        registerCodeExecutionRules();
        registerDeserializationRules();
        registerSandboxEscapeRules();
        registerFileDestructionRules();
        registerPrivilegeEscalationRules();
        registerDataExfiltrationRules();
        registerNetworkAttackRules();
        registerObfuscationRules();
        registerInformationGatheringRules();
        registerMultiLineRules();
    }

    @Override
    public ScriptType supportedType() {
        return ScriptType.PYTHON;
    }

    // ======================== 命令执行 ========================

    private void registerCommandExecutionRules() {
        registerRule(RuleDefinition.of("PY-001", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "os\\.system\\s*\\(", "os.system()执行系统命令", "避免使用os.system()，可能被命令注入"));
        registerRule(RuleDefinition.of("PY-002", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "os\\.popen\\s*\\(", "os.popen()执行系统命令并读取输出", "os.popen()存在命令注入风险"));
        registerRule(RuleDefinition.of("PY-003", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "subprocess\\.(call|Popen|run|check_output|check_call)\\s*\\([^)]*shell\\s*=\\s*True",
                "subprocess使用shell=True执行命令", "shell=True存在严重的命令注入风险"));
        registerRule(RuleDefinition.of("PY-004", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "subprocess\\.(call|Popen|run|check_output|check_call)\\s*\\(",
                "subprocess执行外部命令", "检查执行的命令是否包含用户输入"));
        registerRule(RuleDefinition.of("PY-005", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "subprocess\\.getoutput\\s*\\(", "subprocess.getoutput()执行命令",
                "检查执行的命令是否来自用户输入"));
        registerRule(RuleDefinition.of("PY-006", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "os\\.(execv|execl|execvp|execvpe|execlp|execle)\\s*\\(",
                "os.exec*系列函数替换当前进程", "exec系列调用将替换当前进程"));
        registerRule(RuleDefinition.of("PY-007", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "pty\\.spawn\\s*\\(", "pty.spawn()创建伪终端并执行命令", "可能被用于创建交互式Shell"));
        registerRule(RuleDefinition.of("PY-008", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "commands\\.(getoutput|getstatusoutput)\\s*\\(", "commands模块执行命令（Python2遗留）",
                "commands模块存在命令注入风险"));
        registerRule(RuleDefinition.of("PY-009", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "asyncio\\.create_subprocess_shell\\s*\\(", "asyncio创建shell子进程",
                "shell子进程存在命令注入风险"));
        registerRule(RuleDefinition.of("PY-010", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "asyncio\\.create_subprocess_exec\\s*\\(", "asyncio创建exec子进程",
                "检查执行的命令是否包含用户输入"));
    }

    // ======================== 代码执行 ========================

    private void registerCodeExecutionRules() {
        registerRule(RuleDefinition.of("PY-015", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "eval\\s*\\(", "eval()动态执行Python表达式", "eval()执行不可信输入可能导致任意代码执行"));
        registerRule(RuleDefinition.of("PY-016", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "exec\\s*\\(", "exec()动态执行Python代码", "exec()执行不可信输入可能导致任意代码执行"));
        registerRule(RuleDefinition.of("PY-017", Severity.CRITICAL, FindingCategory.CODE_EXECUTION,
                "compile\\s*\\([^)]*,\\s*'exec'\\s*\\)", "compile()编译为exec模式",
                "compile()配合exec可能导致代码注入"));
        registerRule(RuleDefinition.of("PY-018", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "compile\\s*\\([^)]*,\\s*'eval'\\s*\\)", "compile()编译为eval模式",
                "compile()配合eval可能导致表达式注入"));
        registerRule(RuleDefinition.of("PY-019", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "__import__\\s*\\(\\s*['\"]os['\"]", "__import__('os')动态导入os模块",
                "动态导入os模块可能是沙箱逃逸尝试"));
        registerRule(RuleDefinition.of("PY-020", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "__import__\\s*\\(\\s*['\"](subprocess|sys|ctypes)['\"]",
                "__import__动态导入敏感模块", "动态导入敏感模块可能是沙箱逃逸尝试"));
        registerRule(RuleDefinition.of("PY-021", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "importlib\\.import_module\\s*\\(", "importlib动态导入模块", "动态导入可能存在安全风险"));
        registerRule(RuleDefinition.of("PY-022", Severity.HIGH, FindingCategory.CODE_EXECUTION,
                "code\\.(interact|InteractiveConsole|InteractiveInterpreter)\\s*\\(",
                "code模块创建交互式解释器", "交互式解释器可用于代码注入攻击"));
        registerRule(RuleDefinition.of("PY-023", Severity.MEDIUM, FindingCategory.CODE_EXECUTION,
                "type\\s*\\(\\s*['\"][^'\"]+['\"]\\s*,\\s*\\(", "type()动态创建类",
                "type()动态创建类可能被用于代码注入"));
    }

    // ======================== 反序列化攻击 ========================

    private void registerDeserializationRules() {
        registerRule(RuleDefinition.of("PY-030", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "pickle\\.(loads|load)\\s*\\(", "pickle反序列化（RCE高危）",
                "pickle反序列化不可信数据可导致任意代码执行"));
        registerRule(RuleDefinition.of("PY-031", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "_pickle\\.(loads|load)\\s*\\(", "_pickle反序列化（C实现版）", "反序列化不可信数据可导致RCE"));
        registerRule(RuleDefinition.of("PY-032", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "cPickle\\.(loads|load)\\s*\\(", "cPickle反序列化（Python2）", "cPickle反序列化不可信数据可导致RCE"));
        registerRule(RuleDefinition.of("PY-033", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "marshal\\.(loads|load)\\s*\\(", "marshal反序列化", "marshal反序列化不可信数据也可能存在风险"));
        registerRule(RuleDefinition.of("PY-034", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "yaml\\.(load|full_load|unsafe_load)\\s*\\(", "YAML不安全反序列化",
                "PyYAML的load()函数可以执行任意Python代码"));
        registerRule(RuleDefinition.of("PY-035", Severity.CRITICAL, FindingCategory.DESERIALIZATION,
                "shelve\\.open\\s*\\(.*writeback\\s*=\\s*True",
                "shelve模块writeback=True可能存在风险", "writeback=True可能缓存恶意代码"));
        registerRule(RuleDefinition.of("PY-036", Severity.HIGH, FindingCategory.DESERIALIZATION,
                "jsonpickle\\.(decode|loads)\\s*\\(", "jsonpickle反序列化", "jsonpickle反序列化可能存在安全风险"));
    }

    // ======================== 沙箱逃逸 ========================

    private void registerSandboxEscapeRules() {
        registerRule(RuleDefinition.of("PY-040", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "__subclasses__\\s*\\(\\s*\\)", "获取类的所有子类（沙箱逃逸典型手法）",
                "遍历__subclasses__是Python沙箱逃逸的典型第一步"));
        registerRule(RuleDefinition.of("PY-041", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "__mro__|__bases__|__class__.*__subclasses__",
                "遍历MRO/base/class获取子类（沙箱逃逸）", "通过类层级遍历是沙箱逃逸的典型手法"));
        registerRule(RuleDefinition.of("PY-042", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "getattr\\s*\\(\\s*\\w+\\s*,\\s*['\"]__(subclasses|class|bases|mro|builtins)__['\"]",
                "getattr访问特殊属性（沙箱逃逸）", "通过getattr访问双下划线属性是沙箱逃逸手法"));
        registerRule(RuleDefinition.of("PY-043", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "ctypes\\.(CDLL|cdll|WinDLL|windll|pythonapi|PyDLL)\\s*\\(",
                "ctypes加载动态链接库", "ctypes可以加载任意C库执行系统调用"));
        registerRule(RuleDefinition.of("PY-044", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "cffi\\.FFI\\s*\\(\\s*\\)\\s*\\.\\s*dlopen\\s*\\(",
                "cffi加载动态链接库", "cffi.dlopen可以加载任意系统库"));
        registerRule(RuleDefinition.of("PY-045", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                "__builtins__\\s*\\[|__builtins__\\s*\\.|__builtins__\\s*",
                "访问或修改__builtins__", "修改__builtins__可绕过安全限制"));
        registerRule(RuleDefinition.of("PY-046", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "sys\\.modules\\s*\\[", "访问sys.modules动态获取模块", "通过sys.modules获取受限模块是沙箱逃逸手法"));
        registerRule(RuleDefinition.of("PY-047", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "globals\\s*\\(\\s*\\)\\s*\\[", "通过globals()访问全局变量", "可能用于获取受限对象的引用"));
        registerRule(RuleDefinition.of("PY-048", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "setattr\\s*\\(\\s*\\w+\\s*,\\s*['\"]__", "setattr设置双下划线属性",
                "通过setattr修改特殊属性可突破安全限制"));
        registerRule(RuleDefinition.of("PY-049", Severity.HIGH, FindingCategory.SANDBOX_ESCAPE,
                "ctypes\\.pythonapi\\s*\\.\\s*Py", "调用Python C API", "直接调用Python C API可能绕过安全检查"));
    }

    // ======================== 文件破坏 ========================

    private void registerFileDestructionRules() {
        registerRule(RuleDefinition.of("PY-055", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "shutil\\.rmtree\\s*\\(\\s*['\"]/['\"]", "shutil.rmtree('/')删除根目录",
                "递归删除根目录是不可逆的灾难性操作"));
        registerRule(RuleDefinition.of("PY-056", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "shutil\\.rmtree\\s*\\(\\s*['\"]/(etc|var|usr|boot|home)['\"]",
                "shutil.rmtree删除系统目录", "删除系统关键目录会导致系统不可用"));
        registerRule(RuleDefinition.of("PY-057", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "os\\.(remove|unlink)\\s*\\(\\s*['\"]/(etc|bin|sbin|boot)/",
                "os.remove/os.unlink删除系统文件", "删除系统文件可能导致系统故障"));
        registerRule(RuleDefinition.of("PY-058", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "os\\.rmdir\\s*\\(\\s*['\"]/(etc|var|usr|boot)['\"]",
                "os.rmdir删除系统目录", "删除系统目录会导致系统故障"));
        registerRule(RuleDefinition.of("PY-059", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "pathlib\\.Path\\s*\\(\\s*['\"]/(etc|var|usr)/[^'\"]+['\"]\\s*\\).unlink\\s*\\(",
                "pathlib.Path().unlink()删除系统文件", "删除系统文件可能导致系统故障"));
        registerRule(RuleDefinition.of("PY-060", Severity.MEDIUM, FindingCategory.FILE_DESTRUCTION,
                "open\\s*\\(\\s*['\"]/(etc|var|usr|boot)/[^'\"]+['\"]\\s*,\\s*['\"]w",
                "以写模式打开系统文件", "写入系统文件可能损坏系统配置"));
    }

    // ======================== 权限提升 ========================

    private void registerPrivilegeEscalationRules() {
        registerRule(RuleDefinition.of("PY-065", Severity.CRITICAL, FindingCategory.PRIVILEGE_ESCALATION,
                "os\\.(setuid|seteuid|setreuid|setresuid)\\s*\\(\\s*0\\s*\\)",
                "设置UID为0(root)", "尝试获取root权限"));
        registerRule(RuleDefinition.of("PY-066", Severity.CRITICAL, FindingCategory.PRIVILEGE_ESCALATION,
                "os\\.(setgid|setegid|setregid|setresgid)\\s*\\(\\s*0\\s*\\)",
                "设置GID为0(root组)", "尝试获取root组权限"));
        registerRule(RuleDefinition.of("PY-067", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "os\\.chmod\\s*\\(\\s*['\"]/(etc|bin|sbin|usr)/[^'\"]+['\"].*0o?777",
                "os.chmod设置系统文件为777权限", "系统文件不应设为全局可读写执行"));
        registerRule(RuleDefinition.of("PY-068", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "os\\.chown\\s*\\(\\s*['\"]/(etc|bin|sbin)/", "os.chown变更系统文件所有者",
                "系统文件所有者不应被变更"));
        registerRule(RuleDefinition.of("PY-069", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "os\\.setgroups\\s*\\(", "os.setgroups设置附加组", "修改进程的附加组列表"));
        registerRule(RuleDefinition.of("PY-070", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "resource\\.setrlimit\\s*\\(", "resource.setrlimit修改资源限制",
                "修改资源限制可能被用于DoS攻击准备"));
    }

    // ======================== 数据外泄 ========================

    private void registerDataExfiltrationRules() {
        registerRule(RuleDefinition.of("PY-075", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "requests\\.(post|put)\\s*\\(.*open\\s*\\(", "requests发送文件和读取数据的组合",
                "疑似将本地文件通过HTTP发送到远程"));
        registerRule(RuleDefinition.of("PY-076", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "urllib\\.request\\.urlopen\\s*\\(.*open\\s*\\(", "urllib上传本地文件",
                "疑似将本地文件上传到远程服务器"));
        registerRule(RuleDefinition.of("PY-077", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "ftp(?:lib)?\\.FTP\\s*\\([^)]+\\).*(storbinary|storlines)",
                "FTP上传文件", "检查上传的文件内容是否敏感"));
        registerRule(RuleDefinition.of("PY-078", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "open\\s*\\(\\s*['\"]/(etc/passwd|etc/shadow)[\"']", "读取系统密码文件",
                "读取密码文件通常是为了外泄或破解"));
        registerRule(RuleDefinition.of("PY-079", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "smtp(?:lib)?\\.SMTP\\s*\\([^)]+\\).*sendmail", "SMTP发送邮件（数据外泄通道）",
                "邮件可能用于外泄数据"));
        registerRule(RuleDefinition.of("PY-080", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "http\\.(server|client)\\s*", "启动HTTP服务或客户端", "HTTP服务可能被用于数据外泄"));
    }

    // ======================== 网络攻击 ========================

    private void registerNetworkAttackRules() {
        registerRule(RuleDefinition.of("PY-085", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "socket\\.socket\\s*\\([^)]*\\)[\\s\\S]{0,200}\\.(connect|bind)\\s*\\(",
                "Python Socket + connect/bind（疑似反弹Shell）", "Socket编程结合命令执行即为反弹Shell"));
        registerRule(RuleDefinition.of("PY-086", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "os\\.dup2\\s*\\(", "os.dup2文件描述符重定向（反弹Shell核心）",
                "dup2重定向文件描述符是反弹Shell的核心技术"));
        registerRule(RuleDefinition.of("PY-087", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "scapy\\.", "Scapy网络数据包操控库", "Scapy可用于网络攻击和嗅探"));
        registerRule(RuleDefinition.of("PY-088", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "paramiko\\.(SSHClient|Transport|SFTPClient)\\s*\\(",
                "Paramiko SSH客户端", "检查SSH连接的目标和认证方式"));
        registerRule(RuleDefinition.of("PY-089", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "impacket\\.", "Impacket Windows网络协议库", "Impacket常用于Windows域渗透测试"));
        registerRule(RuleDefinition.of("PY-090", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "socket\\.(gethostname|gethostbyname|getaddrinfo)\\s*\\(", "网络信息收集",
                "DNS查询和主机信息收集"));
    }

    // ======================== 代码混淆/隐藏 ========================

    private void registerObfuscationRules() {
        registerRule(RuleDefinition.of("PY-095", Severity.HIGH, FindingCategory.OBFUSCATION,
                "(?:base64|binascii|codecs)\\.(?:b64decode|a85decode|decode)\\s*\\([^)]*\\)[\\s\\S]{0,100}(?:eval|exec)",
                "Base64解码后执行", "解码后执行是混淆恶意代码的常见模式"));
        registerRule(RuleDefinition.of("PY-096", Severity.HIGH, FindingCategory.OBFUSCATION,
                "compile\\s*\\([^)]+,\\s*[^)]+,\\s*['\"]exec['\"]\\s*\\)", "compile编译为可执行代码",
                "compile配合exec是常见的代码隐藏手法"));
        registerRule(RuleDefinition.of("PY-097", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "getattr\\s*\\([^)]+,\\s*['\"][^'\"]+['\"]\\s*\\)\\s*\\(.*\\)",
                "getattr动态获取方法并调用", "动态方法调用可能隐藏真实意图"));
        registerRule(RuleDefinition.of("PY-098", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "chr\\s*\\(\\d+\\)\\s*\\+", "chr()字符拼接构建字符串", "chr()拼接可能是混淆技术"));
        registerRule(RuleDefinition.of("PY-099", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "lambda\\s+\\w+\\s*:\\s*(eval|exec|__import__|os\\.system)",
                "lambda中包含危险函数", "lambda中隐藏危险调用"));
    }

    // ======================== 信息收集 ========================

    private void registerInformationGatheringRules() {
        registerRule(RuleDefinition.of("PY-100", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/(etc/passwd|etc/shadow)['\"]", "读取系统密码文件",
                "密码文件包含用户账户敏感信息"));
        registerRule(RuleDefinition.of("PY-101", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/etc/(hosts|resolv\\.conf|fstab)['\"]", "读取系统配置文件",
                "系统配置文件包含网络和挂载信息"));
        registerRule(RuleDefinition.of("PY-102", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "os\\.(environ|getenv\\s*\\()", "读取系统环境变量",
                "环境变量可能包含密钥和敏感配置"));
        registerRule(RuleDefinition.of("PY-103", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/proc/(cpuinfo|meminfo|version|modules)['\"]", "读取/proc系统信息",
                "内核信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("PY-104", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "platform\\.(uname|system|release|version|machine|processor|architecture)\\s*\\(",
                "platform模块获取系统信息", "系统信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("PY-105", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](ifconfig|ip\\s+(addr|link))", "subprocess执行网络接口命令",
                "网络拓扑信息收集"));

        // 防火墙规则探测
        registerRule(RuleDefinition.of("PY-106", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](iptables|nft|ufw)", "subprocess执行防火墙查询命令",
                "防火墙规则暴露网络防御策略，禁止AI探测"));

        // 进程列表探测
        registerRule(RuleDefinition.of("PY-107", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](ps|pgrep|pidof)", "subprocess执行进程查询命令",
                "进程列表暴露运行中的服务和安全软件"));

        // 网络连接探测
        registerRule(RuleDefinition.of("PY-108", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](netstat|ss|lsof)", "subprocess执行网络连接查询命令",
                "网络连接信息暴露通信拓扑和服务端口"));

        // 环境变量和敏感配置
        registerRule(RuleDefinition.of("PY-109", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"].*\\.env['\"]|dotenv\\.load_dotenv|load_dotenv\\s*\\(",
                "读取.env环境变量文件", ".env文件通常包含密钥、Token等敏感凭证"));
        registerRule(RuleDefinition.of("PY-110", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"].*/(\\.ssh|\\.aws|\\.kube|\\.docker|\\.config)/",
                "读取SSH/AWS/K8s/Docker密钥目录", "读取密钥和凭证文件是明确的信息窃取行为"));
        registerRule(RuleDefinition.of("PY-111", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/etc/(sudoers|security/|selinux/|apparmor/)['\"]",
                "读取安全配置文件", "安全配置文件暴露系统防御机制"));

        // 系统日志探测
        registerRule(RuleDefinition.of("PY-112", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/var/log/", "读取系统日志文件",
                "系统日志可能包含敏感操作记录和凭证信息"));

        // 内核模块探测
        registerRule(RuleDefinition.of("PY-113", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](lsmod|systemctl|service)", "subprocess执行内核模块/服务查询命令",
                "内核模块列表和服务状态可用于检测安全监控"));

        // 定时任务探测
        registerRule(RuleDefinition.of("PY-114", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"]crontab|open\\s*\\(\\s*['\"]/etc/cron", "查看定时任务",
                "定时任务可能暴露系统自动化流程和脚本路径"));

        // 历史命令探测
        registerRule(RuleDefinition.of("PY-115", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"].*/(\\.bash_history|\\.zsh_history|\\.mysql_history|\\.psql_history|\\.python_history)",
                "读取Shell历史记录", "历史记录可能包含密码、密钥等敏感操作"));

        // 磁盘挂载信息
        registerRule(RuleDefinition.of("PY-116", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "os\\.(statvfs|getcwd\\s*\\(\\s*\\)|listdir\\s*\\(\\s*['\"]/)|shutil\\.disk_usage",
                "获取磁盘/目录信息", "磁盘挂载信息暴露存储拓扑"));

        // 用户和组信息
        registerRule(RuleDefinition.of("PY-117", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "open\\s*\\(\\s*['\"]/etc/group['\"]|grp\\.", "查看用户和组信息",
                "用户列表暴露系统账户信息"));

        // 容器环境探测
        registerRule(RuleDefinition.of("PY-118", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](docker|kubectl|podman|crictl)", "subprocess执行容器命令",
                "容器环境信息暴露基础设施架构"));

        // 敏感文件搜索
        registerRule(RuleDefinition.of("PY-119", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "glob\\.glob\\s*\\(.*\\.(pem|key|crt|cer|pfx|p12|jks|keystore)", "搜索密钥和证书文件",
                "搜索密钥文件是明确的凭证窃取行为"));
        registerRule(RuleDefinition.of("PY-120", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "glob\\.glob\\s*\\(.*\\.(env|ini|conf|config|yml|yaml|json|properties|toml)",
                "搜索配置文件", "批量搜索配置文件可能用于信息收集"));

        // 内核参数探测
        registerRule(RuleDefinition.of("PY-121", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "subprocess\\..*['\"](sysctl|env|printenv|set\\b)", "subprocess执行内核参数/环境变量命令",
                "内核参数和环境变量暴露系统配置"));

        // psutil全库探测
        registerRule(RuleDefinition.of("PY-122", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "psutil\\.", "psutil库收集系统信息",
                "psutil可获取进程、网络、用户等全面敏感系统信息"));
    }

    // ======================== 多行规则 ========================

    private void registerMultiLineRules() {
        registerMultiLineRule(new RuleDefinition("PY-M001", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                Pattern.compile("import\\s+socket[\\s\\S]{0,300}(subprocess|os\\.(system|popen|dup2))",
                        Pattern.DOTALL),
                "Socket + Subprocess 反弹Shell组合", "典型的Python反弹Shell模式"));

        registerMultiLineRule(new RuleDefinition("PY-M002", Severity.CRITICAL, FindingCategory.SANDBOX_ESCAPE,
                Pattern.compile("__subclasses__\\(\\)[\\s\\S]{0,500}(__init__|__globals__|__builtins__)",
                        Pattern.DOTALL),
                "__subclasses__ + 属性遍历 沙箱逃逸链", "完整的Python沙箱逃逸攻击链"));

        registerMultiLineRule(new RuleDefinition("PY-M003", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                Pattern.compile("open\\s*\\(\\s*['\"]/(etc|root)[^'\"]+['\"][\\s\\S]{0,300}(requests\\.post|urlopen|smtplib)",
                        Pattern.DOTALL),
                "读取敏感文件 + 网络发送 数据外泄链", "敏感信息读取并外泄的组合模式"));

        registerMultiLineRule(new RuleDefinition("PY-M004", Severity.CRITICAL, FindingCategory.OBFUSCATION,
                Pattern.compile("(base64|codecs)\\.(b64decode|decode)[\\s\\S]{0,200}(eval|exec|compile)",
                        Pattern.DOTALL),
                "解码 + 执行 混淆代码链", "解码后立即执行的混淆代码模式"));

        // 敏感文件读取 + 网络外传 信息窃取链
        registerMultiLineRule(new RuleDefinition("PY-M005", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("open\\s*\\(\\s*['\"].*(\\.env|/etc/(passwd|shadow|sudoers)|\\.ssh/|\\.aws/|\\.kube/).*\\n.*(requests\\.(post|put)|urlopen|smtplib\\.SMTP)",
                        Pattern.DOTALL),
                "读取敏感文件后通过网络外传", "典型的Python敏感信息窃取链"));

        // 系统信息全面收集链
        registerMultiLineRule(new RuleDefinition("PY-M006", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("(platform\\.(uname|system)|subprocess\\.(run|Popen|call))[\\s\\S]{0,300}(os\\.environ|os\\.getenv|open.*\\.env)",
                        Pattern.DOTALL),
                "批量收集系统信息+环境变量", "系统信息全面收集是攻击前置侦察行为"));
    }

}
