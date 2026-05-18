package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent 侧 MCP 工具暴露模式
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum McpToolExposureMode {
    ALL_GLOBAL("继承 MCP 全局可用工具"),
    SELECTED_ONLY("仅暴露局部勾选工具");

    private final String description;
}
