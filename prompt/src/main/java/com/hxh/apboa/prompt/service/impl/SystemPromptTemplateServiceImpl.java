package com.hxh.apboa.prompt.service.impl;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.hxh.apboa.common.entity.SystemPromptTemplate;
import com.hxh.apboa.prompt.mapper.SystemPromptTemplateMapper;
import com.hxh.apboa.prompt.service.SystemPromptTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统提示词模板Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class SystemPromptTemplateServiceImpl extends ServiceImpl<SystemPromptTemplateMapper, SystemPromptTemplate> implements SystemPromptTemplateService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        ArrayList<Object> names = new ArrayList<>();
        getAgentDefinitions(ids).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    public List<String> listCategories() {
        return this.lambdaQuery()
                .select(SystemPromptTemplate::getCategory)
                .isNotNull(SystemPromptTemplate::getCategory)
                .groupBy(SystemPromptTemplate::getCategory)
                .list()
                .stream()
                .map(SystemPromptTemplate::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toList());
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> systemPromptId) {
        if (systemPromptId == null || systemPromptId.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = systemPromptId.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM agent_definition WHERE system_prompt_template_id IN (%s)", subSql);
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
