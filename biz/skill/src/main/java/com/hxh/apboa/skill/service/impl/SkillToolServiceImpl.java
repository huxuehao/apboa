package com.hxh.apboa.skill.service.impl;

import com.hxh.apboa.common.entity.SkillTool;
import com.hxh.apboa.skill.mapper.SkillToolMapper;
import com.hxh.apboa.skill.service.SkillToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 技能工具关联Service实现
 *
 * @author huxuehao
 */
@Service
public class SkillToolServiceImpl extends ServiceImpl<SkillToolMapper, SkillTool> implements SkillToolService {

    @Override
    public List<Long> getToolIds(Long skillId) {
        return lambdaQuery()
                .eq(SkillTool::getSkillId, skillId)
                .list()
                .stream()
                .map(SkillTool::getToolId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getSkillIds(List<Long> toolIds) {
        return lambdaQuery()
                .in(SkillTool::getToolId, toolIds)
                .list()
                .stream()
                .map(SkillTool::getSkillId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Boolean saveSkillTool(Long skillId, List<Long> toolIds) {
        deleteSkillTool(List.of(skillId));
        if (toolIds != null && !toolIds.isEmpty()) {
            toolIds.forEach(toolId -> {
                save(new SkillTool(null, skillId, toolId));
            });
        }
        return true;
    }

    @Override
    public Boolean deleteSkillTool(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(SkillTool::getSkillId, skillIds).remove();
    }
}
