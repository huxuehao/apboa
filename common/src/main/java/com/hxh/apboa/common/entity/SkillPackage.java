package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.consts.TableConst;
import lombok.Getter;
import lombok.Setter;

/**
 * 技能包
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.SKILL, autoResultMap = true)
public class SkillPackage extends BaseEntity {

    /**
     * 技能包名称
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 技能内容（概述）
     */
    private String skillContent;

    /**
     * 技能分类
     */
    private String category;

    /**
     * 资源列表
     */
    @TableField(value = "`references`",typeHandler = JsonNodeTypeHandler.class)
    private JsonNode references;

    /**
     * 示例列表
     */
    @TableField(value = "`examples`",typeHandler = JsonNodeTypeHandler.class)
    private JsonNode examples;

    /**
     * 脚本列表
     */
    @TableField(value = "`scripts`",typeHandler = JsonNodeTypeHandler.class)
    private JsonNode scripts;
}
