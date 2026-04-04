package com.hxh.apboa.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.agent.mapper.CodeExecutionConfigMapper;
import com.hxh.apboa.agent.service.AgentCodeExecutionService;
import com.hxh.apboa.agent.service.CodeExecutionConfigService;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.CodeExecutionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：CodeExecutionConfigServiceImpl
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class CodeExecutionConfigServiceImpl extends ServiceImpl<CodeExecutionConfigMapper, CodeExecutionConfig> implements CodeExecutionConfigService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentCodeExecutionService agentCodeExecutionService;

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        getAgentDefinitions(agentCodeExecutionService.getAgentIds(ids)).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    /**
     * 根据Agent ID列表获取Agent定义列表
     *
     * @param agentIds Agent ID列表
     * @return Agent定义列表
     */
    private List<AgentDefinition> getAgentDefinitions(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = agentIds.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM %s WHERE id IN (%s)", TableConst.AGENT, subSql);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            AgentDefinition agent = new AgentDefinition();
            agent.setId(rs.getLong("id"));
            agent.setName(rs.getString("name"));
            agent.setDescription(rs.getString("description"));
            return agent;
        });
    }
}
