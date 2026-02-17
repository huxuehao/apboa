package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.SerializableEnable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 智能体与技能包关联
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("agent_skill_packages")
@AllArgsConstructor
@NoArgsConstructor
public class AgentSkillPackage implements SerializableEnable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long agentDefinitionId;
    private Long skillPackageId;
}
