package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP协议
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum McpProtocol {
    HTTP("HTTP"),
    SSE("SSE"),
    STDIO("StdIO");

    private final String description;
}
