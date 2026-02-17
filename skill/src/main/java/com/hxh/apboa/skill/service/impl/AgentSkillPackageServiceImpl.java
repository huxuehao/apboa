package com.hxh.apboa.skill.service.impl;

import com.hxh.apboa.common.entity.AgentSkillPackage;
import com.hxh.apboa.skill.mapper.AgentSkillPackageMapper;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体技能包关联Service实现
 *
 * @author huxuehao
 */
@Service
public class AgentSkillPackageServiceImpl extends ServiceImpl<AgentSkillPackageMapper, AgentSkillPackage> implements AgentSkillPackageService {
    @Override
    public List<Long> getAgentIds(List<Long> skillIds) {
        return lambdaQuery()
                .in(AgentSkillPackage::getSkillPackageId, skillIds)
                .list()
                .stream()
                .map(AgentSkillPackage::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getSkillPackageIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentSkillPackage::getAgentDefinitionId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentSkillPackage::getSkillPackageId)
                .toList();
    }

    @Override
    public Boolean insertAgentSkillPackage(Long agentDefinitionId, List<Long> skillPackageIds) {
        skillPackageIds.forEach(skillPackageId -> {
            save(new AgentSkillPackage(null, agentDefinitionId, skillPackageId));
        });

        return true;
    }

    @Override
    public Boolean deleteAgentSkillPackage(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentSkillPackage::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentSkillPackage(Long agentDefinitionId, List<Long> skillPackageIds) {
        deleteAgentSkillPackage(List.of(agentDefinitionId));
        insertAgentSkillPackage(agentDefinitionId, skillPackageIds);

        return Boolean.TRUE;
    }
}
