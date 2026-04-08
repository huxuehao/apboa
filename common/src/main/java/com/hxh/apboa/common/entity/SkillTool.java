package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.consts.TableConst;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 智能体与工具关联
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(TableConst.SKILL_TOOL)
@AllArgsConstructor
@NoArgsConstructor
public class SkillTool implements SerializableEnable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long skillId;
    private Long toolId;
}
