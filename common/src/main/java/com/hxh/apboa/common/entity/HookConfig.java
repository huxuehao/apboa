package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.enums.HookType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * Hook配置表
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = "hook_config")
public class HookConfig extends BaseEntity {

    /**
     * Hook名称
     */
    private String name;

    /**
     * Hook类型
     */
    private HookType hookType;

    /**
     * 描述
     */
    private String description;

    /**
     * hook路径（tool_type为SYSTEM时使用）
     */
    private String classPath;

    /**
     * hook内容（tool_type为CUSTOM时使用）
     */
    private String hookContent;

    /**
     * 执行优先级
     */
    private Integer priority;
}
