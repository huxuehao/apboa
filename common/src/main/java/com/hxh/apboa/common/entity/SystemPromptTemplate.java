package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 系统提示词模板
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("system_prompt_template")
public class SystemPromptTemplate extends BaseEntity {

    /**
     * 分类
     */
    private String category;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 使用次数统计
     */
    private Integer usageCount;
}
