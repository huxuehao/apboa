package com.hxh.apboa.skill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.AgentSkillPackage;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.skill.mapper.SkillPackageMapper;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 技能包Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class SkillPackageServiceImpl extends ServiceImpl<SkillPackageMapper, SkillPackage> implements SkillPackageService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentSkillPackageService agentSkillPackageService;

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        getAgentDefinitions(agentSkillPackageService.getAgentIds(ids)).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    public List<String> listCategories() {
        return this.lambdaQuery()
                .select(SkillPackage::getCategory)
                .isNotNull(SkillPackage::getCategory)
                .groupBy(SkillPackage::getCategory)
                .list()
                .stream()
                .map(SkillPackage::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        removeByIds(ids);
        return agentSkillPackageService.remove(new LambdaQueryWrapper<AgentSkillPackage>().in(AgentSkillPackage::getSkillPackageId, ids));
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = agentIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM %s WHERE id IN (%s)", TableConst.AGENT, subSql);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AgentDefinition agent = new AgentDefinition();
            // 手动映射字段
            agent.setId(rs.getLong("id"));
            agent.setName(rs.getString("name"));
            agent.setDescription(rs.getString("description"));
            return agent;
        });
    }
}
