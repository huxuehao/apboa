package com.hxh.apboa.security.script.checker;

import com.hxh.apboa.security.script.AbstractScriptChecker;
import com.hxh.apboa.security.script.model.FindingCategory;
import com.hxh.apboa.security.script.model.RuleDefinition;
import com.hxh.apboa.security.script.model.ScriptType;
import com.hxh.apboa.security.script.model.Severity;

import java.util.regex.Pattern;

/**
 * 描述：Shell/Bash 脚本安全检查器，覆盖文件破坏、反弹Shell、命令注入、权限提升、
 * 资源耗尽、数据外泄、持久化驻留、服务操控、代码混淆、内核模块、网络攻击等全面攻击面。
 *
 * @author huxuehao
 */
public class ShellSecurityChecker extends AbstractScriptChecker {

    public ShellSecurityChecker() {
        registerFileDestructionRules();
        registerReverseShellRules();
        registerCommandInjectionRules();
        registerPrivilegeEscalationRules();
        registerResourceExhaustionRules();
        registerDataExfiltrationRules();
        registerPersistenceRules();
        registerServiceManipulationRules();
        registerObfuscationRules();
        registerKernelModuleRules();
        registerNetworkAttackRules();
        registerInformationGatheringRules();
        registerMultiLineRules();
    }

    @Override
    public ScriptType supportedType() {
        return ScriptType.SHELL;
    }

    // ======================== 文件系统破坏 ========================

    private void registerFileDestructionRules() {
        // rm 危险操作
        registerRule(RuleDefinition.of("SH-001", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "rm\\s+.*-rf\\s+/", "递归强制删除根目录", "绝对不要删除根目录 /"));
        registerRule(RuleDefinition.of("SH-002", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "rm\\s+.*-rf\\s+/\\*", "递归强制删除根目录下所有文件", "不应删除根目录下的所有内容"));
        registerRule(RuleDefinition.of("SH-003", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "rm\\s+.*--no-preserve-root", "使用 --no-preserve-root 标志危险删除", "移除 --no-preserve-root，此标志极危险"));
        registerRule(RuleDefinition.of("SH-004", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "rm\\s+.*-rf\\s+~/", "递归强制删除用户主目录", "请指定更精确的删除路径"));
        registerRule(RuleDefinition.of("SH-005", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "rm\\s+.*-rf\\s+(/etc|/var|/boot|/usr|/opt|/home)", "递归强制删除关键系统目录",
                "不应删除系统关键目录"));

        // 磁盘/文件系统破坏
        registerRule(RuleDefinition.of("SH-006", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "mkfs\\.", "格式化文件系统命令", "格式化操作将不可逆地清除数据"));
        registerRule(RuleDefinition.of("SH-007", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "mkswap\\s+/dev/", "在磁盘设备上创建swap分区", "此操作会覆盖磁盘数据"));
        registerRule(RuleDefinition.of("SH-008", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "dd\\s+if=.*of=/dev/sd", "dd写入块设备，可能摧毁分区表", "不应直接写入块设备"));
        registerRule(RuleDefinition.of("SH-009", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                ">\s*/(dev/sd|dev/hd|dev/xvd|dev/vd)", "重定向输出覆盖磁盘设备", "不可逆的磁盘数据破坏"));
        registerRule(RuleDefinition.of("SH-010", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "cat\\s+/dev/zero\\s*>\\s*/dev/", "写入零数据到磁盘设备", "这会导致磁盘数据永久丢失"));
        registerRule(RuleDefinition.of("SH-011", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "shred\\s+", "安全删除文件工具shred", "请确认删除文件是否必要"));
        registerRule(RuleDefinition.of("SH-012", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "wipefs\\s+", "擦除文件系统签名", "此操作可能使分区不可用"));
        registerRule(RuleDefinition.of("SH-013", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "sfdisk\\s+", "磁盘分区表操作工具", "不当操作会损坏分区表"));
        registerRule(RuleDefinition.of("SH-014", Severity.HIGH, FindingCategory.FILE_DESTRUCTION,
                "parted\\s+.*rm", "parted删除分区", "分区删除后数据不可恢复"));
        registerRule(RuleDefinition.of("SH-015", Severity.CRITICAL, FindingCategory.FILE_DESTRUCTION,
                "cp\\s+/dev/(zero|null)\\s+/dev/sd", "复制设备节点覆盖磁盘", "此操作会永久损坏磁盘数据"));
    }

    // ======================== 反弹Shell ========================

    private void registerReverseShellRules() {
        registerRule(RuleDefinition.of("SH-020", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "bash\\s+-i\\s*>&\\s*/dev/tcp/", "Bash反弹Shell到远程IP", "反弹Shell是明确的后门行为"));
        registerRule(RuleDefinition.of("SH-021", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "/(dev/tcp|dev/udp)/", "使用 /dev/tcp 或 /dev/udp 建立网络连接", "疑似反弹Shell通信"));
        registerRule(RuleDefinition.of("SH-022", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "nc\\s+.*-e\\s+/bin/(sh|bash)", "Netcat反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-023", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "nc\\s+.*-c\\s+/bin/(sh|bash)", "Netcat -c 参数反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-024", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "ncat\\s+.*-e\\s+", "Ncat反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-025", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "socat\\s+.*exec:", "Socat反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-026", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "python\\d*\\s+-c\\s+.*socket", "Python反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-027", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "(?i)perl\\s+-e\\s+.*socket", "Perl反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-028", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "ruby\\s+-e\\s+.*(TCPSocket|socket)", "Ruby反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-029", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "php\\s+-r\\s+.*fsockopen", "PHP反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-030", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "telnet\\s+.*/bin/(sh|bash)", "Telnet反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-031", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "exec\\s+\\d+<>/dev/tcp/", "exec重定向 + /dev/tcp 反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-032", Severity.HIGH, FindingCategory.REVERSE_SHELL,
                "nc\\s+-l\\w*\\s+-p\\s+\\d+", "Netcat监听端口", "不明目的的端口监听可能是后门"));
        registerRule(RuleDefinition.of("SH-033", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "lua\\s+-e\\s+.*socket", "Lua反弹Shell", "反弹Shell是严重的安全威胁"));
        registerRule(RuleDefinition.of("SH-034", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                "groovy\\s+-e\\s+.*connect", "Groovy反弹Shell", "反弹Shell是严重的安全威胁"));
    }

    // ======================== 命令注入 ========================

    private void registerCommandInjectionRules() {
        registerRule(RuleDefinition.of("SH-040", Severity.CRITICAL, FindingCategory.COMMAND_INJECTION,
                "eval\\s+\\$", "eval执行变量内容（潜在命令注入）", "避免使用eval执行未验证的变量内容"));
        registerRule(RuleDefinition.of("SH-041", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "`[^`]*(rm|nc|curl|wget|chmod|chown|dd)[^`]*`", "反引号内包含危险命令", "检查反引号中的命令是否来自用户输入"));
        registerRule(RuleDefinition.of("SH-042", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "\\$\\([^)]*(rm|nc|curl|wget|chmod|dd)[^)]*\\)", "$() 命令替换中包含危险命令",
                "检查 $() 中的命令是否来自用户输入"));
        registerRule(RuleDefinition.of("SH-043", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "xargs\\s+.*(rm|sh|bash|eval)", "xargs 管道执行危险操作", "xargs执行命令时存在注入风险"));
        registerRule(RuleDefinition.of("SH-044", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "find\\s+.*-exec\\s+(rm|sh|bash|chmod)", "find -exec 执行危险命令", "检查 -exec 后的命令是否安全"));
        registerRule(RuleDefinition.of("SH-045", Severity.MEDIUM, FindingCategory.COMMAND_INJECTION,
                "awk\\s+.*system\\s*\\(", "awk调用system()执行命令", "awk的system()可能执行注入的命令"));
        registerRule(RuleDefinition.of("SH-046", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "source\\s+\\$", "source执行变量指定的文件", "检查变量内容是否可能被用户控制"));
        registerRule(RuleDefinition.of("SH-047", Severity.HIGH, FindingCategory.COMMAND_INJECTION,
                "\\$\\(<", "命令替换读取文件内容", "文件内容可能被恶意控制"));
        registerRule(RuleDefinition.of("SH-048", Severity.MEDIUM, FindingCategory.COMMAND_INJECTION,
                "IFS=.*read", "修改IFS后读取输入", "IFS修改可能导致命令解析异常"));
    }

    // ======================== 权限提升 ========================

    private void registerPrivilegeEscalationRules() {
        registerRule(RuleDefinition.of("SH-055", Severity.CRITICAL, FindingCategory.PRIVILEGE_ESCALATION,
                "chmod\\s+777\\s+/", "设置根目录为777权限", "严重的安全风险，任何人都可读写执行根目录"));
        registerRule(RuleDefinition.of("SH-056", Severity.CRITICAL, FindingCategory.PRIVILEGE_ESCALATION,
                "chmod\\s+-R\\s+777\\s+/(etc|var|usr|bin|sbin|boot)", "递归设置系统目录777权限",
                "系统目录不应设为全局可写"));
        registerRule(RuleDefinition.of("SH-057", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "chmod\\s+[0-7]*7[0-7]*7\\s+/(etc|bin|sbin|usr|boot)", "设置系统目录为全局可写",
                "系统关键目录不应设为全局可写"));
        registerRule(RuleDefinition.of("SH-058", Severity.CRITICAL, FindingCategory.PRIVILEGE_ESCALATION,
                "chown\\s+-R\\s+\\w+:\\w+\\s+/(etc|bin|sbin|usr|boot|var)", "递归变更系统目录所有者",
                "系统目录所有权不应被变更"));
        registerRule(RuleDefinition.of("SH-059", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "chattr\\s+-i\\s+/(etc|bin|sbin)", "移除关键文件的不可变属性", "移除不可变属性后文件可被篡改"));
        registerRule(RuleDefinition.of("SH-060", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "chmod\\s+u\\+s\\s+/bin/", "设置SUID位在系统二进制上", "SUID位可能导致权限提升"));
        registerRule(RuleDefinition.of("SH-061", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "setfacl\\s+.*/(etc|bin|sbin)", "在系统目录上设置ACL", "ACL变更可能扩大访问权限"));
        registerRule(RuleDefinition.of("SH-062", Severity.HIGH, FindingCategory.PRIVILEGE_ESCALATION,
                "sudo\\s+.*(su|bash|sh|zsh)", "sudo切换到Shell", "检查此操作是否有业务必要性"));
        registerRule(RuleDefinition.of("SH-063", Severity.MEDIUM, FindingCategory.PRIVILEGE_ESCALATION,
                "chmod\\s+[0-7]*[46]00\\s+", "设置SUID/SGID位", "SUID/SGID位可能导致权限提升"));
    }

    // ======================== 资源耗尽 ========================

    private void registerResourceExhaustionRules() {
        registerRule(RuleDefinition.of("SH-070", Severity.CRITICAL, FindingCategory.RESOURCE_EXHAUSTION,
                ":\\s*\\(\\s*\\)\\s*\\{\\s*:\\s*\\|\\s*:?\\s*&\\s*\\}\\s*;\\s*:", "Fork炸弹 — 经典的 :(){ :|:& };: 模式",
                "Fork炸弹会导致系统资源耗尽宕机"));
        registerRule(RuleDefinition.of("SH-071", Severity.CRITICAL, FindingCategory.RESOURCE_EXHAUSTION,
                "\\w+\\(\\)\\s*\\{\\s*\\w+\\|\\w+\\s*&\\s*\\}\\s*;\\s*\\w+", "Fork炸弹变体",
                "Fork炸弹会导致系统资源耗尽宕机"));
        registerRule(RuleDefinition.of("SH-072", Severity.HIGH, FindingCategory.RESOURCE_EXHAUSTION,
                "cat\\s+/dev/zero\\s*>", "将 /dev/zero 数据写入文件或管道（可能填满磁盘）",
                "/dev/zero 无限写入会迅速占满磁盘"));
        registerRule(RuleDefinition.of("SH-073", Severity.MEDIUM, FindingCategory.RESOURCE_EXHAUSTION,
                "yes\\s+\\|", "yes 命令无限输出到管道", "yes命令无限输出可能消耗大量CPU和内存"));
        registerRule(RuleDefinition.of("SH-074", Severity.HIGH, FindingCategory.RESOURCE_EXHAUSTION,
                "dd\\s+if=/dev/(zero|urandom)\\s+of=.*bs=\\S+\\s+count=0", "dd无限写入（count=0跳过限制）",
                "无限写入会迅速占满磁盘"));
        registerRule(RuleDefinition.of("SH-075", Severity.MEDIUM, FindingCategory.RESOURCE_EXHAUSTION,
                "while\\s+true\\s*;\\s*do\\s+.*fork", "死循环中执行fork", "无限fork子进程会耗尽PID资源"));
        registerRule(RuleDefinition.of("SH-076", Severity.HIGH, FindingCategory.RESOURCE_EXHAUSTION,
                "ulimit\\s+-u\\s+unlimited", "解除最大进程数限制", "解除限制后可能被用于fork炸弹"));
    }

    // ======================== 数据外泄 ========================

    private void registerDataExfiltrationRules() {
        registerRule(RuleDefinition.of("SH-080", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                "curl\\s+.*\\|\\s*(ba)?sh", "curl下载内容直接管道给Shell执行", "远程下载执行是最常见的攻击链之一"));
        registerRule(RuleDefinition.of("SH-081", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                "wget\\s+.*-O\\s*-.*\\|\\s*(ba)?sh", "wget下载内容直接管道给Shell执行",
                "远程下载执行是最常见的攻击链之一"));
        registerRule(RuleDefinition.of("SH-082", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                "curl\\s+.*\\|\\s*(python|perl|ruby|php)", "curl下载内容管道给脚本解释器",
                "远程下载执行是最常见的攻击链之一"));
        registerRule(RuleDefinition.of("SH-083", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "curl\\s+.*-F\\s+.*@(/etc/|/root/|/home/)", "curl上传敏感文件到远程服务器", "检查上传的文件是否包含敏感信息"));
        registerRule(RuleDefinition.of("SH-084", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "scp\\s+/(etc/|root/).*\\w+@", "scp复制敏感文件到远程", "检查复制的文件是否包含敏感信息"));
        registerRule(RuleDefinition.of("SH-085", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "rsync\\s+.*/(etc/|root/).*:", "rsync同步敏感文件到远程", "检查同步的文件是否包含敏感信息"));
        registerRule(RuleDefinition.of("SH-086", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "nc\\s+.*<\\s+/(etc/|root/)", "nc传输敏感文件到远程", "疑似数据外泄"));
        registerRule(RuleDefinition.of("SH-087", Severity.HIGH, FindingCategory.DATA_EXFILTRATION,
                "base64\\s+/(etc/passwd|etc/shadow)", "对敏感文件进行base64编码（外泄前准备）",
                "对密码文件编码通常是外泄的前置步骤"));
        registerRule(RuleDefinition.of("SH-088", Severity.MEDIUM, FindingCategory.DATA_EXFILTRATION,
                "curl\\s+.*-d\\s+.*\\$\\(cat\\s+/(etc/|root/)", "curl POST提交敏感文件内容", "疑似数据外泄"));
    }

    // ======================== 持久化驻留 ========================

    private void registerPersistenceRules() {
        registerRule(RuleDefinition.of("SH-095", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(curl|wget|nc|bash\\s+-i)\\s*.*crontab|crontab\\s+.*(curl|wget|nc|bash\\s+-i)", "crontab中写入恶意定时任务", "定时任务可能被用于持久化后门"));
        registerRule(RuleDefinition.of("SH-096", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*/etc/cron", "写入系统cron目录", "系统cron目录中的文件会被自动执行"));
        registerRule(RuleDefinition.of("SH-097", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*/etc/rc\\.local", "写入rc.local自启动脚本", "rc.local在系统启动时自动执行"));
        registerRule(RuleDefinition.of("SH-098", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*~/\\.bashrc", "追加内容到 .bashrc（Shell启动时执行）",
                "检查是否有恶意命令被写入.bashrc"));
        registerRule(RuleDefinition.of("SH-099", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*~/\\.(bash_)?profile", "追加内容到profile文件",
                "profile文件在登录时自动执行"));
        registerRule(RuleDefinition.of("SH-100", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*/etc/profile", "写入系统级profile", "系统profile影响所有用户"));
        registerRule(RuleDefinition.of("SH-101", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*/etc/init\\.d/", "写入init.d服务脚本", "init.d脚本会在系统启动时执行"));
        registerRule(RuleDefinition.of("SH-102", Severity.HIGH, FindingCategory.PERSISTENCE,
                "systemctl\\s+enable\\s+", "启用systemd服务（可能的持久化）", "检查启用的服务是否为已知安全服务"));
        registerRule(RuleDefinition.of("SH-103", Severity.HIGH, FindingCategory.PERSISTENCE,
                "(echo|cat|tee)\\s+.*>>?\\s*~/.ssh/authorized_keys", "写入SSH authorized_keys（后门访问）",
                "未验证的公钥写入可能导致未授权SSH访问"));
        registerRule(RuleDefinition.of("SH-104", Severity.HIGH, FindingCategory.PERSISTENCE,
                "at\\s+", "at命令调度一次性任务", "检查at命令调度的内容"));
        registerRule(RuleDefinition.of("SH-105", Severity.HIGH, FindingCategory.PERSISTENCE,
                "systemctl\\s+.*daemon-reload", "重新加载systemd守护进程", "检查是否有恶意服务被安装"));
    }

    // ======================== 系统服务操控 ========================

    private void registerServiceManipulationRules() {
        registerRule(RuleDefinition.of("SH-110", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "systemctl\\s+stop\\s+(firewall|iptables|ufw)", "停止防火墙服务", "关闭防火墙将使系统暴露于攻击"));
        registerRule(RuleDefinition.of("SH-111", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "systemctl\\s+disable\\s+(firewall|iptables|ufw|selinux|apparmor)",
                "禁用安全服务", "禁用安全服务将永久削弱系统防御"));
        registerRule(RuleDefinition.of("SH-112", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "service\\s+(iptables|firewall|ufw)\\s+(stop|disable)", "停止或禁用安全服务",
                "关闭防火墙将使系统暴露于攻击"));
        registerRule(RuleDefinition.of("SH-113", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "iptables\\s+-F", "清空iptables规则", "清空防火墙规则将使系统失去网络防护"));
        registerRule(RuleDefinition.of("SH-114", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "iptables\\s+-P\\s+(INPUT|OUTPUT|FORWARD)\\s+ACCEPT", "设置iptables默认策略为ACCEPT",
                "这意味着所有流量将被放行"));
        registerRule(RuleDefinition.of("SH-115", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "setenforce\\s+0", "禁用SELinux", "关闭SELinux会削弱系统的强制访问控制"));
        registerRule(RuleDefinition.of("SH-116", Severity.HIGH, FindingCategory.SERVICE_MANIPULATION,
                "systemctl\\s+mask\\s+", "mask systemd服务（阻止启动）", "检查mask的服务是否合法"));
        registerRule(RuleDefinition.of("SH-117", Severity.CRITICAL, FindingCategory.SERVICE_MANIPULATION,
                "ufw\\s+disable", "禁用UFW防火墙", "关闭防火墙将使系统暴露于攻击"));
        registerRule(RuleDefinition.of("SH-118", Severity.HIGH, FindingCategory.SERVICE_MANIPULATION,
                "kill\\s+-9\\s+-1", "kill -9 -1 杀死所有进程", "这将终止系统中所有可终止的进程"));
    }

    // ======================== 代码混淆 ========================

    private void registerObfuscationRules() {
        registerRule(RuleDefinition.of("SH-120", Severity.CRITICAL, FindingCategory.OBFUSCATION,
                "base64\\s+.*-d\\s*\\|\\s*(ba)?sh", "Base64解码后管道给Shell执行", "混淆执行的恶意代码"));
        registerRule(RuleDefinition.of("SH-121", Severity.CRITICAL, FindingCategory.OBFUSCATION,
                "openssl\\s+.*-d\\b.*\\|\\s*(ba)?sh", "OpenSSL解密后管道给Shell执行", "加密混淆的恶意代码"));
        registerRule(RuleDefinition.of("SH-122", Severity.CRITICAL, FindingCategory.OBFUSCATION,
                "xxd\\s+-r\\s+-p\\s*\\|\\s*(ba)?sh", "Hex解码后管道给Shell执行", "十六进制混淆的恶意代码"));
        registerRule(RuleDefinition.of("SH-123", Severity.HIGH, FindingCategory.OBFUSCATION,
                "\\w+=\"[^\"]*\";\\s*eval\\s+\\$", "变量赋值后eval执行", "常见的混淆执行模式"));
        registerRule(RuleDefinition.of("SH-124", Severity.HIGH, FindingCategory.OBFUSCATION,
                "tr\\s+.*\\|\\s*(ba)?sh", "tr字符替换后执行", "字符替换混淆的恶意代码"));
        registerRule(RuleDefinition.of("SH-125", Severity.HIGH, FindingCategory.OBFUSCATION,
                "sed\\s+.*\\|\\s*(ba)?sh", "sed处理后管道给Shell", "sed可能用于混淆和解码恶意代码"));
        registerRule(RuleDefinition.of("SH-126", Severity.HIGH, FindingCategory.OBFUSCATION,
                "awk\\s+.*\\|\\s*(ba)?sh", "awk处理后管道给Shell", "awk可能用于解码恶意代码"));
        registerRule(RuleDefinition.of("SH-127", Severity.HIGH, FindingCategory.OBFUSCATION,
                "printf\\s+.*\\\\x[0-9a-fA-F]{2}.*\\|\\s*(ba)?sh", "printf十六进制解码后执行",
                "printf编码的恶意Shell代码"));
        registerRule(RuleDefinition.of("SH-128", Severity.MEDIUM, FindingCategory.OBFUSCATION,
                "\\$'\\\\x[0-9a-fA-F]{2}", "$'\\xNN' 十六进制转义", "Shell字符串中的十六进制编码"));
    }

    // ======================== 内核模块 ========================

    private void registerKernelModuleRules() {
        registerRule(RuleDefinition.of("SH-135", Severity.CRITICAL, FindingCategory.KERNEL_MODULE,
                "insmod\\s+", "加载内核模块", "未验证的内核模块可能包含rootkit"));
        registerRule(RuleDefinition.of("SH-136", Severity.CRITICAL, FindingCategory.KERNEL_MODULE,
                "rmmod\\s+", "卸载内核模块", "可能试图卸载安全监控模块"));
        registerRule(RuleDefinition.of("SH-137", Severity.CRITICAL, FindingCategory.KERNEL_MODULE,
                "modprobe\\s+", "加载/卸载内核模块", "未验证的内核模块可能包含rootkit"));
        registerRule(RuleDefinition.of("SH-138", Severity.HIGH, FindingCategory.KERNEL_MODULE,
                "(echo|cat|tee)\\s+.*>>?\\s*/sys/", "写入/sys目录（修改内核参数）",
                "/sys是内核参数接口，写入可能改变系统行为"));
        registerRule(RuleDefinition.of("SH-139", Severity.HIGH, FindingCategory.KERNEL_MODULE,
                "sysctl\\s+-w\\s+", "修改内核运行时参数", "不当的sysctl参数可能影响系统安全"));
    }

    // ======================== 网络攻击 ========================

    private void registerNetworkAttackRules() {
        registerRule(RuleDefinition.of("SH-145", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "tcpdump\\s+", "网络抓包工具tcpdump", "网络嗅探可能捕获敏感信息"));
        registerRule(RuleDefinition.of("SH-146", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "tshark\\s+", "TShark网络分析工具", "网络嗅探可能捕获敏感信息"));
        registerRule(RuleDefinition.of("SH-147", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "arpspoof\\s+", "ARP欺骗工具", "ARP欺骗用于中间人攻击"));
        registerRule(RuleDefinition.of("SH-148", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "ettercap\\s+", "Ettercap中间人攻击工具", "中间人攻击工具"));
        registerRule(RuleDefinition.of("SH-149", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "ssh\\s+-L\\s+", "SSH本地端口转发", "端口转发可能绕过防火墙规则"));
        registerRule(RuleDefinition.of("SH-150", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "ssh\\s+-R\\s+", "SSH远程端口转发", "远程端口转发可能暴露内网服务"));
        registerRule(RuleDefinition.of("SH-151", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "iptables\\s+-t\\s+nat\\s+-A", "iptables NAT规则（端口转发/重定向）", "检查端口转发目的"));
        registerRule(RuleDefinition.of("SH-152", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "nmap\\s+", "Nmap网络扫描工具", "端口扫描可能是攻击的前置侦察"));
        registerRule(RuleDefinition.of("SH-153", Severity.HIGH, FindingCategory.NETWORK_ATTACK,
                "masscan\\s+", "Masscan高速端口扫描", "高速端口扫描通常是攻击行为"));
    }

    // ======================== 信息收集 ========================

    private void registerInformationGatheringRules() {
        registerRule(RuleDefinition.of("SH-160", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "cat\\s+/(etc/passwd|etc/shadow)", "读取系统密码文件", "密码文件包含用户账户敏感信息"));
        registerRule(RuleDefinition.of("SH-161", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(cat|grep|less|head|tail)\\s+/etc/(hosts|resolv\\.conf|fstab)", "读取系统配置文件",
                "系统配置文件包含网络和挂载信息"));
        registerRule(RuleDefinition.of("SH-162", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "env\\s*\\|\\s*(grep|sort)", "列出所有环境变量", "环境变量可能包含密钥和敏感配置"));
        registerRule(RuleDefinition.of("SH-163", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "cat\\s+/proc/(cpuinfo|meminfo|version|modules)", "读取/proc系统信息",
                "内核信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("SH-164", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "uname\\s+-a", "获取系统信息", "系统信息收集可能是攻击前置步骤"));
        registerRule(RuleDefinition.of("SH-165", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "ifconfig\\s+-a|ip\\s+addr\\s+show", "获取网络接口信息", "网络拓扑信息收集"));

        // 防火墙规则探测
        registerRule(RuleDefinition.of("SH-166", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "iptables\\s+(-L|--list|-S)", "查看iptables防火墙规则",
                "防火墙规则暴露网络防御策略，禁止AI探测"));
        registerRule(RuleDefinition.of("SH-167", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "nft\\s+list\\s+ruleset|ufw\\s+status", "查看nftables/ufw防火墙规则",
                "防火墙状态暴露系统安全配置"));

        // 进程列表探测
        registerRule(RuleDefinition.of("SH-168", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "ps\\s+(aux|aux|ax|ef|-ef|--forest|axf)", "列出系统所有进程",
                "进程列表暴露运行中的服务和安全软件"));
        registerRule(RuleDefinition.of("SH-169", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "pgrep\\s+|pidof\\s+", "按名称搜索进程",
                "进程搜索可能用于定位安全软件"));

        // 网络连接探测
        registerRule(RuleDefinition.of("SH-170", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "netstat\\s+-[a-z]*[anltup]+|ss\\s+-[a-z]*[anltup]+", "查看网络连接状态",
                "网络连接信息暴露通信拓扑和服务端口"));
        registerRule(RuleDefinition.of("SH-171", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "lsof\\s+-i", "列出网络相关的打开文件", "网络连接和端口信息泄露"));

        // 环境变量和敏感配置
        registerRule(RuleDefinition.of("SH-172", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "cat\\s+.*\\.env\\b|cat\\s+/etc/environment", "读取环境变量配置文件",
                ".env文件通常包含密钥、Token等敏感凭证"));
        registerRule(RuleDefinition.of("SH-173", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "(cat|ls|find)\\s+~?/\\.(ssh|aws|config|kube|docker)/|(cat|ls|find)\\s+/root/\\.(ssh|aws|kube)/",
                "读取SSH/AWS/K8s/Docker密钥目录", "读取密钥和凭证文件是明确的信息窃取行为"));
        registerRule(RuleDefinition.of("SH-174", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "cat\\s+/etc/(sudoers|security/|selinux/|apparmor/)", "读取安全配置文件",
                "安全配置文件暴露系统防御机制"));

        // 系统日志探测
        registerRule(RuleDefinition.of("SH-175", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(cat|tail|head|less|grep)\\s+/var/log/", "读取系统日志文件",
                "系统日志可能包含敏感操作记录和凭证信息"));

        // 内核模块探测
        registerRule(RuleDefinition.of("SH-176", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "lsmod\\s*|cat\\s+/proc/modules", "列出已加载内核模块",
                "内核模块列表可用于检测安全监控模块"));

        // 系统服务探测
        registerRule(RuleDefinition.of("SH-177", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "systemctl\\s+(list-units|status|show)|service\\s+--status-all", "查看系统服务状态",
                "服务列表暴露系统运行的应用和版本"));

        // 定时任务探测
        registerRule(RuleDefinition.of("SH-178", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "crontab\\s+-l|ls\\s+/etc/cron", "查看定时任务",
                "定时任务可能暴露系统自动化流程和脚本路径"));

        // 历史命令探测
        registerRule(RuleDefinition.of("SH-179", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "(cat|tail|head)\\s+~?/\\.(bash_history|zsh_history|mysql_history|psql_history|python_history|node_repl_history)",
                "读取Shell历史记录", "历史记录可能包含密码、密钥等敏感操作"));

        // 磁盘挂载信息
        registerRule(RuleDefinition.of("SH-180", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "df\\s+-[a-z]*h|cat\\s+/proc/mounts|lsblk\\s+", "查看磁盘挂载信息",
                "磁盘挂载信息暴露存储拓扑"));

        // 用户和组信息
        registerRule(RuleDefinition.of("SH-181", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "cat\\s+/etc/group|getent\\s+(passwd|group|shadow)", "查看用户和组信息",
                "用户列表暴露系统账户信息"));

        // 容器环境探测
        registerRule(RuleDefinition.of("SH-182", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "docker\\s+(info|ps|inspect|images)|kubectl\\s+(get|describe)|podman\\s+(info|ps)",
                "探测容器环境信息", "容器环境信息暴露基础设施架构"));

        // 敏感文件搜索
        registerRule(RuleDefinition.of("SH-183", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                "find\\s+/.*-name\\s+.*\\.(pem|key|crt|cer|pfx|p12|jks|keystore)", "搜索密钥和证书文件",
                "搜索密钥文件是明确的凭证窃取行为"));
        registerRule(RuleDefinition.of("SH-184", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "find\\s+/.*-name\\s+.*\\.(env|ini|conf|config|yml|yaml|json|properties|toml)",
                "搜索配置文件", "批量搜索配置文件可能用于信息收集"));

        // 内核参数探测
        registerRule(RuleDefinition.of("SH-185", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                "sysctl\\s+-a", "查看所有内核运行时参数",
                "内核参数暴露系统调优和安全配置"));
    }

    // ======================== 多行规则（跨行攻击链检测） ========================

    private void registerMultiLineRules() {
        // 下载-执行链
        registerMultiLineRule(new RuleDefinition("SH-M001", Severity.CRITICAL, FindingCategory.DATA_EXFILTRATION,
                Pattern.compile("(curl|wget)\\s+\\S+.*\\n.*(chmod\\s+\\+x|chmod\\s+777).*\\n.*\\./\\S+",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE),
                "下载-加权限-执行 完整攻击链", "典型的恶意软件下载执行链"));

        // 反弹Shell准备链
        registerMultiLineRule(new RuleDefinition("SH-M002", Severity.CRITICAL, FindingCategory.REVERSE_SHELL,
                Pattern.compile("python\\d*\\s+-c\\s+.*import\\s+socket.*connect", Pattern.DOTALL),
                "Python反弹Shell完整代码", "反弹Shell是严重的安全威胁"));

        // 持久化链
        registerMultiLineRule(new RuleDefinition("SH-M003", Severity.CRITICAL, FindingCategory.PERSISTENCE,
                Pattern.compile("(crontab|echo.*cron).*\\n.*(curl|wget|nc|/dev/tcp)", Pattern.DOTALL),
                "定时任务+远程访问 持久化链", "定时任务执行远程代码是典型的后门"));

        // pkill/pkillall 危险操作
        registerMultiLineRule(new RuleDefinition("SH-M004", Severity.HIGH, FindingCategory.SERVICE_MANIPULATION,
                Pattern.compile("(killall|pkill)\\s+(sshd|systemd|cron|syslog)", Pattern.CASE_INSENSITIVE),
                "终止关键系统服务进程", "终止sshd/systemd/cron可能导致系统不可用或失去远程管理能力"));

        // 敏感文件读取 + 网络外传 信息窃取链
        registerMultiLineRule(new RuleDefinition("SH-M005", Severity.CRITICAL, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("(cat|readfile)\\s+.*(\\.env|/etc/(passwd|shadow|sudoers)|\\.ssh/|\\.aws/|\\.kube/).*\\n.*(curl|wget|nc|scp|rsync)",
                        Pattern.DOTALL),
                "读取敏感文件后通过网络外传", "典型的敏感信息窃取链"));

        // 系统信息全面收集链
        registerMultiLineRule(new RuleDefinition("SH-M006", Severity.HIGH, FindingCategory.INFORMATION_GATHERING,
                Pattern.compile("(uname|ps\\s+aux|netstat|iptables\\s+-L|ss\\s+-).*\\n.*(env|printenv|cat\\s+.*\\.env)",
                        Pattern.DOTALL),
                "批量收集系统信息", "系统信息全面收集是攻击前置侦察行为"));
    }

}
