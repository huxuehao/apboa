package com.hxh.apboa.mcp.service;

/**
 * MCP 运行时自动降级服务
 *
 * @author huxuehao
 */
public interface McpRuntimeDegradeService {

    /**
     * 记录一次运行时成功。
     *
     * @param serverId MCP 服务 ID
     * @param activationRevision 激活版本号
     * @param configHash 配置哈希
     * @param runtimeFailThreshold 自动降级阈值
     */
    void recordSuccess(Long serverId,
                       Long activationRevision,
                       String configHash,
                       Integer runtimeFailThreshold);

    /**
     * 记录一次运行时失败。
     *
     * @param serverId MCP 服务 ID
     * @param activationRevision 激活版本号
     * @param configHash 配置哈希
     * @param runtimeFailThreshold 自动降级阈值
     * @param throwable 失败原因
     */
    void recordFailure(Long serverId,
                       Long activationRevision,
                       String configHash,
                       Integer runtimeFailThreshold,
                       Throwable throwable);
}
