package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 失败来源
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum McpFailureSource {
    NONE("无"),
    RUNTIME_AUTO_DEGRADE("运行时自动降级");

    private final String description;
}
