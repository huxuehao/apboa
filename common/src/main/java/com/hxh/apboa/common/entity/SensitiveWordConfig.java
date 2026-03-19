package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.SensitiveWordAction;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 敏感词配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.SENSITIVE_WORD, autoResultMap = true)
public class SensitiveWordConfig extends BaseEntity {

    /**
     * 分类
     */
    private String category;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 敏感词列表
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode words;

    /**
     * 处理动作
     */
    private SensitiveWordAction action;

    /**
     * 替换文本
     */
    private String replacement;
}
