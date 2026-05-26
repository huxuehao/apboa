package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = TableConst.SKILL)
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
     * 技能分类
     */
    private String category;
}
