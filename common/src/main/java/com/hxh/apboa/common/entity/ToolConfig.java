package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.enums.CodeLanguage;
import com.hxh.apboa.common.enums.ToolType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 工具表
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = "tool_config", autoResultMap = true)
public class ToolConfig extends BaseEntity {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具编号
     */
    private String toolId;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具分类
     */
    private String category;

    /**
     * 工具类型: 内置/自定义
     */
    private ToolType toolType;

    /**
     * 是否需要确认
     */
    private Boolean needConfirm;

    /**
     * 输入参数schema
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode inputSchema;

    /**
     * 输出格式schema
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode outputSchema;

    /**
     * 工具路径（tool_type为SYSTEM时使用）
     */
    private String classPath;

    /**
     * 代码语言（tool_type为CUSTOM时使用）
     */
    private CodeLanguage language;

    /**
     * 代码（tool_type为CUSTOM时使用）
     */
    private String code;

    /**
     * 版本号
     */
    private String version;
}
