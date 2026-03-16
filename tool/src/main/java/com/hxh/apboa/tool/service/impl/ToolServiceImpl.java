package com.hxh.apboa.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.AgentTool;
import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.enums.ToolType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.wrapper.ToolInfoWrapper;
import com.hxh.apboa.tool.mapper.ToolMapper;
import com.hxh.apboa.tool.service.AgentToolService;
import com.hxh.apboa.tool.service.ToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工具Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class ToolServiceImpl extends ServiceImpl<ToolMapper, ToolConfig> implements ToolService {
    private final JdbcTemplate jdbcTemplate;
    private final AgentToolService agentToolService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTools(List<Long> ids) {
        listByIds(ids).forEach(toolConfig -> {
            if (toolConfig.getToolType() != ToolType.BUILTIN) {
                removeById(toolConfig.getId());
            }
        });

        agentToolService.remove(new LambdaQueryWrapper<AgentTool>().in(AgentTool::getToolId, ids));

        return true;
    }

    @Override
    public void SyncConfigToDatabase(List<ToolInfoWrapper> toolInfos) {
        lambdaUpdate()
                .notIn(ToolConfig::getClassPath, toolInfos.stream().map(ToolInfoWrapper::getClassPath).toList())
                .eq(ToolConfig::getToolType, ToolType.BUILTIN)
                .remove();
        toolInfos.forEach(toolInfo -> {
            List<ToolConfig> list = lambdaQuery()
                    .eq(ToolConfig::getToolType, ToolType.BUILTIN)
                    .eq(ToolConfig::getClassPath, toolInfo.getClassPath())
                    .list();
            if (list.isEmpty()) {
                ToolConfig toolConfig = new ToolConfig();
                toolConfig.setName(toolInfo.getName());
                toolConfig.setToolId(toolInfo.getName());
                toolConfig.setToolType(ToolType.BUILTIN);
                toolConfig.setCategory("内置");
                toolConfig.setDescription(toolInfo.getDescription());
                toolConfig.setClassPath(toolInfo.getClassPath());
                toolConfig.setInputSchema(JsonUtils.toJsonNode(toolInfo.getParams()));
                toolConfig.setEnabled(true);
                save(toolConfig);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    if (i == 0) {
                        list.get(i).setToolId(toolInfo.getName());
                        list.get(i).setToolType(ToolType.BUILTIN);
                        list.get(i).setClassPath(toolInfo.getClassPath());
                        list.get(i).setInputSchema(JsonUtils.toJsonNode(toolInfo.getParams()));
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
        getAgentDefinitions(agentToolService.getAgentIds(ids)).forEach(agentDefinition -> {
            names.add(agentDefinition.getName());
        });

        return names;
    }

    @Override
    public List<String> listCategories() {
        return this.lambdaQuery()
                .select(ToolConfig::getCategory)
                .isNotNull(ToolConfig::getCategory)
                .groupBy(ToolConfig::getCategory)
                .list()
                .stream()
                .map(ToolConfig::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public Boolean doUpdate(ToolConfig toolConfig) {
        if (toolConfig.getToolType() != ToolType.BUILTIN) {
            return updateById(toolConfig);
        }

        return lambdaUpdate()
                .eq(ToolConfig::getId, toolConfig.getId())
                .set(ToolConfig::getName, toolConfig.getName())
                .set(ToolConfig::getCategory, toolConfig.getCategory())
                .set(ToolConfig::getDescription, toolConfig.getDescription())
                .set(ToolConfig::getNeedConfirm, toolConfig.getNeedConfirm())
                .set(ToolConfig::getVersion, toolConfig.getVersion())
                .update();
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
