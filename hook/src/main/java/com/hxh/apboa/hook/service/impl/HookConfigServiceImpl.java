package com.hxh.apboa.hook.service.impl;

import com.hxh.apboa.common.entity.AgentDefinition;
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
