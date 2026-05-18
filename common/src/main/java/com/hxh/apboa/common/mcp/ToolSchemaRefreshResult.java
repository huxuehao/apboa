package com.hxh.apboa.common.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具目录刷新结果
 *
 * @author huxuehao
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolSchemaRefreshResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 工具目录 JSON
     */
    private String toolSchemas;

    /**
     * 工具数量
     */
    private int toolCount;

    /**
     * 结果说明
     */
    private String message;
}
