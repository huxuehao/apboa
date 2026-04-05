package com.hxh.apboa.model.service.impl;

import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.entity.ModelProvider;
import com.hxh.apboa.common.wrapper.ModelWrapper;
import com.hxh.apboa.model.mapper.ModelConfigMapper;
import com.hxh.apboa.model.service.ModelConfigService;
import com.hxh.apboa.model.service.ModelProviderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型配置Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {
    private final JdbcTemplate jdbcTemplate;
    private final ModelProviderService modelProviderService;
    private final MessagePublisher messagePublisher;

    @Override
    public ModelWrapper getModelWrapperById(Long id) {
        ModelConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("model config not found");
        }

        if (!config.getEnabled()) {
            throw new RuntimeException("model config is disabled");
        }

        ModelProvider modelProvider = modelProviderService.getById(config.getProviderId());
        if (modelProvider == null) {
            throw new RuntimeException("model provider not found");
        }

        if (!modelProvider.getEnabled()) {
            throw new RuntimeException("model provider is disabled");
        }

        return ModelWrapper.builder()
                .config(config)
                .provider(modelProvider)
                .build();
    }

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        ArrayList<Object> names = new ArrayList<>();
        getAgentDefinitions(ids).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    public boolean deleteByIds(List<Long> ids) {
        // 删除前先获取关联的智能体ID，以便后续触发重新注册
        List<Long> agentIds = getAgentDefinitions(ids).stream().map(AgentDefinition::getId).toList();
        boolean result = removeByIds(ids);
        publishAgentReregister(agentIds);
        return result;
    }

    @Override
    public boolean doUpdate(ModelConfig entity) {
        boolean result = updateById(entity);
        List<Long> agentIds = getAgentDefinitions(List.of(entity.getId())).stream().map(AgentDefinition::getId).toList();
        publishAgentReregister(agentIds);
        return result;
    }

    private void publishAgentReregister(List<Long> agentIds) {
        agentIds.forEach(agentId ->
                messagePublisher.publish(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(agentId)));
    }

    private List<AgentDefinition> getAgentDefinitions(List<Long> systemPromptId) {
        if (systemPromptId == null || systemPromptId.isEmpty()) {
            return new ArrayList<>();
        }

        String subSql = systemPromptId.stream().map(String::valueOf).collect(Collectors.joining(","));

        String sql = String.format("SELECT * FROM %s WHERE model_config_id IN (%s)", TableConst.AGENT, subSql);
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
