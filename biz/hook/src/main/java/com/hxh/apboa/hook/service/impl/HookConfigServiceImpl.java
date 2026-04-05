package com.hxh.apboa.hook.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.AgentHook;
import com.hxh.apboa.common.entity.HookConfig;
import com.hxh.apboa.common.enums.HookType;
import com.hxh.apboa.common.wrapper.HookConfigWrapper;
import com.hxh.apboa.hook.mapper.HookConfigMapper;
import com.hxh.apboa.hook.service.AgentHookService;
import com.hxh.apboa.hook.service.HookConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hook配置Service实现
 *
 * @author huxuehao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HookConfigServiceImpl extends ServiceImpl<HookConfigMapper, HookConfig> implements HookConfigService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentHookService agentHookService;
    private final MessagePublisher messagePublisher;

    @Override
    public void SyncConfigToDatabase(List<HookConfigWrapper> configWrappers) {
        lambdaUpdate().notIn(HookConfig::getClassPath, configWrappers.stream().map(HookConfigWrapper::getClassPath).toList())
                .isNotNull(HookConfig::getClassPath)
                .remove();
        configWrappers.forEach(configWrapper -> {
            List<HookConfig> list = lambdaQuery().eq(HookConfig::getClassPath, configWrapper.getClassPath()).list();
            if (list.isEmpty()) {
                HookConfig hookConfig = new HookConfig();
                hookConfig.setName(configWrapper.getName());
                hookConfig.setHookType(HookType.BUILTIN);
                hookConfig.setDescription(configWrapper.getDescription());
                hookConfig.setClassPath(configWrapper.getClassPath());
                hookConfig.setEnabled(true);
                hookConfig.setPriority(1);
                save(hookConfig);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    if (i == 0) {
                        list.get(i).setHookType(HookType.BUILTIN);
                        list.get(i).setClassPath(configWrapper.getClassPath());
                        updateById(list.get(i));
                    } else {
                        removeById(list.get(i));
                    }
                }
            }
        });
    }

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        getAgentDefinitions(agentHookService.getAgentIds(ids)).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByIds(List<Long> ids) {
        // 删除前先获取关联的智能体ID，以便后续触发重新注册
        List<Long> agentIds = agentHookService.getAgentIds(ids);
        removeByIds(ids);
        boolean result = agentHookService.remove(new LambdaQueryWrapper<AgentHook>().in(AgentHook::getHookConfigId, ids));
        publishAgentReregister(agentIds);
        return result;
    }

    @Override
    public boolean doUpdate(HookConfig entity) {
        boolean result = updateById(entity);
        publishAgentReregister(agentHookService.getAgentIds(List.of(entity.getId())));
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
