package com.hxh.apboa.knowledge.service.impl;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.knowledge.mapper.KnowledgeBaseConfigMapper;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库配置Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseConfigServiceImpl extends ServiceImpl<KnowledgeBaseConfigMapper, KnowledgeBaseConfig> implements KnowledgeBaseConfigService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentKnowledgeBaseServiceImpl agentKnowledgeBaseService;

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        getAgentDefinitions(agentKnowledgeBaseService.getAgentIds(ids)).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    public KnowledgeBaseConfig getByAgentId(Long agentId) {
        List<Long> knowledgeIds = agentKnowledgeBaseService.getKnowledgeIds(agentId);

        if (knowledgeIds.isEmpty()) {
            return null;
        }

        List<KnowledgeBaseConfig> knowledgeBaseConfigs = listByIds(knowledgeIds);
        if (knowledgeBaseConfigs == null) {
            return null;
        }

        return knowledgeBaseConfigs.getFirst();
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = agentIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM agent_definition WHERE id IN (%s)", subSql);
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
