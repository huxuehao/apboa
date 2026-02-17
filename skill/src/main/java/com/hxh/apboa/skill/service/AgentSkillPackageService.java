package com.hxh.apboa.skill.service;

import com.hxh.apboa.common.entity.AgentSkillPackage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 智能体技能包关联Service
 *
 * @author huxuehao
 */
public interface AgentSkillPackageService extends IService<AgentSkillPackage> {
    List<Long> getAgentIds(List<Long> skillIds);
    List<Long> getSkillPackageIds(Long agentDefinitionId);
    Boolean insertAgentSkillPackage(Long agentDefinitionId, List<Long> skillPackageIds);
    Boolean deleteAgentSkillPackage(List<Long> agentIds);
    Boolean saveAgentSkillPackage(Long agentDefinitionId, List<Long> skillPackageIds);
}
