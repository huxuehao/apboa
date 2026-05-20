package com.hxh.apboa.security.script.model;

/**
 * 描述：不安全行为的分类枚举
 *
 * @author huxuehao
 */
public enum FindingCategory {

    /** 文件系统破坏 — rm -rf、格式化、删除关键文件 */
    FILE_DESTRUCTION("文件破坏"),

    /** 命令注入 — eval、exec、动态命令拼接 */
    COMMAND_INJECTION("命令注入"),

    /** 反弹Shell — 建立远程控制通道 */
    REVERSE_SHELL("反弹Shell"),

    /** 代码执行 — 动态代码执行、沙箱逃逸 */
    CODE_EXECUTION("代码执行"),

    /** 权限提升 — chmod 777、setuid(0) 等 */
    PRIVILEGE_ESCALATION("权限提升"),

    /** 资源耗尽 — Fork炸弹、无限写入 */
    RESOURCE_EXHAUSTION("资源耗尽"),

    /** 数据外泄 — 敏感文件上传、curl外传数据 */
    DATA_EXFILTRATION("数据外泄"),

    /** 反序列化攻击 — pickle、marshal 反序列化RCE */
    DESERIALIZATION("反序列化攻击"),

    /** 沙箱逃逸 — getattr subclass遍历、vm沙箱突破 */
    SANDBOX_ESCAPE("沙箱逃逸"),

    /** 代码混淆 — base64解码执行、hex编码绕过 */
    OBFUSCATION("代码混淆"),

    /** 持久化驻留 — crontab、rc.local 等自启动 */
    PERSISTENCE("持久化驻留"),

    /** 网络攻击 — 端口监听、网络扫描、中间人 */
    NETWORK_ATTACK("网络攻击"),

    /** 系统服务操控 — 停止防火墙、禁用安全服务 */
    SERVICE_MANIPULATION("服务操控"),

    /** 内核模块加载 — insmod、modprobe 加载内核模块 */
    KERNEL_MODULE("内核模块"),

    /** XSS跨站脚本 — HTML/JS注入攻击 */
    XSS("跨站脚本"),

    /** 信息收集 — 读取系统信息、网络拓扑 */
    INFORMATION_GATHERING("信息收集");

    private final String label;

    FindingCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
