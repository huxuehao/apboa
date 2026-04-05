package com.hxh.apboa.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.AgentKnowledgeBase;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.knowledge.mapper.KnowledgeBaseConfigMapper;
import com.hxh.apboa.knowledge.service.KnowledgeBaseConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MessagePublisher messagePublisher;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        // 删除前先获取关联的智能体ID，以便后续触发重新注册
        List<Long> agentIds = agentKnowledgeBaseService.getAgentIds(ids);
        removeByIds(ids);
        boolean result = agentKnowledgeBaseService.remove(new LambdaQueryWrapper<AgentKnowledgeBase>().in(AgentKnowledgeBase::getKnowledgeBaseConfigId, ids));
        publishAgentReregister(agentIds);
        return result;
    }

    @Override
    public boolean doUpdate(KnowledgeBaseConfig entity) {
        boolean result = updateById(entity);
        publishAgentReregister(agentKnowledgeBaseService.getAgentIds(List.of(entity.getId())));
        return result;
    }

    private void publishAgentReregister(List<Long> agentIds) {
        agentIds.forEach(agentId ->
                messagePublisher.publish(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(agentId)));
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
