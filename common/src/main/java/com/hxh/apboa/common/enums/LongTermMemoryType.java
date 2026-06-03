package com.hxh.apboa.common.enums;

/**
 * 长期记忆类型枚举
 * <p>
 * 定义支持的长期记忆实现方式，便于扩展新的记忆类型。
 *
 * @author wei.liu
 */
public enum LongTermMemoryType {
    /**
     * Mem0 长期记忆（https://mem0.ai）
     * 支持 Platform 和 Self-hosted 两种部署方式
     */
    MEM0,

    /**
     * ReMe 长期记忆（阿里通义千问记忆能力）
     * 基于阿里云通义千问的记忆服务
     */
    REME,

    /**
     * 百炼记忆库（阿里云百炼平台）
     * 基于阿里云百炼平台的记忆库服务
     */
    BAILIAN
}
