package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP运行模式
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum McpMode {
    SYNC("同步"),
    ASYNC("异步");

    private final String description;
}
