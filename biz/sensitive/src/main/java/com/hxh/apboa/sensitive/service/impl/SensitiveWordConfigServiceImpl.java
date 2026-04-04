package com.hxh.apboa.sensitive.service.impl;

import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.hxh.apboa.sensitive.mapper.SensitiveWordConfigMapper;
import com.hxh.apboa.sensitive.service.SensitiveWordConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 敏感词配置Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class SensitiveWordConfigServiceImpl extends ServiceImpl<SensitiveWordConfigMapper, SensitiveWordConfig> implements SensitiveWordConfigService {
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
                .select(SensitiveWordConfig::getCategory)
                .isNotNull(SensitiveWordConfig::getCategory)
                .groupBy(SensitiveWordConfig::getCategory)
                .list()
                .stream()
                .map(SensitiveWordConfig::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toList());
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> systemPromptId) {
        if (systemPromptId == null || systemPromptId.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = systemPromptId.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM %s WHERE sensitive_word_config_id IN (%s)", TableConst.AGENT, subSql);
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
