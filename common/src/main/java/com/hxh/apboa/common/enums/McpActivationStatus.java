package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MCP 激活状态
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum McpActivationStatus {
    NOT_ACTIVATED("未激活"),
    ACTIVATING("激活中"),
    ACTIVE("已激活"),
    FAILED("激活失败");

    private final String description;
}
