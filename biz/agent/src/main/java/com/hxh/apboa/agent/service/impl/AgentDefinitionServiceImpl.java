package com.hxh.apboa.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.a2a.service.AgentA2aService;
import com.hxh.apboa.agent.mapper.AgentDefinitionMapper;
import com.hxh.apboa.agent.mapper.IJobInfoMapper;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.agent.service.AgentSubAgentService;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.entity.*;
import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.enums.ModelType;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.hxh.apboa.common.vo.SkillPackageVO;
import com.hxh.apboa.common.vo.ToolVO;
import com.hxh.apboa.hook.service.AgentHookService;
import com.hxh.apboa.knowledge.service.AgentKnowledgeBaseService;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.model.service.ModelConfigService;
import com.hxh.apboa.params.core.ParamsAdapter;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.hxh.apboa.studio.service.AgentStudioService;
import com.hxh.apboa.tool.service.AgentToolService;
import com.hxh.apboa.agent.service.AgentCodeExecutionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.tool.service.ToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体定义Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class AgentDefinitionServiceImpl extends ServiceImpl<AgentDefinitionMapper, AgentDefinition> implements AgentDefinitionService {
    private final AgentHookService agentHookService;
    private final AgentToolService agentToolService;
    private final ToolService toolService;
    private final AgentMcpServerService agentMcpServerService;
    private final AgentSkillPackageService agentSkillPackageService;
    private final SkillPackageService skillPackageService;
    private final AgentSubAgentService agentSubAgentService;
    private final AgentKnowledgeBaseService agentKnowledgeBaseService;
    private final ModelConfigService modelConfigService;
    private final ParamsAdapter paramsAdapter;
    private final AgentA2aService agentA2aService;
    private final AgentStudioService agentStudioService;
    private final IJobInfoMapper iJobInfoMapper;
    private final AgentCodeExecutionService agentCodeExecutionService;
    private final MessagePublisher messagePublisher;

    @Override
    public AgentDefinitionVO agentDefinitionDetail(Long id) {
        AgentDefinition entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("AgentDefinition not found for id: " + id);
        }

        AgentDefinitionVO vo = BeanUtils.copy(entity, AgentDefinitionVO.class);

        vo.setHook(agentHookService.getHookIds(id));
        Long studioConfigId = agentStudioService.getStudioIdByAgentId(id);
        if (studioConfigId != null) {
            vo.setStudioConfigId(studioConfigId);
        }
        Long codeExecutionId = agentCodeExecutionService.getCodeExecutionIdByAgentId(id);
        if (codeExecutionId != null) {
            vo.setCodeExecutionConfigId(codeExecutionId);
        }

        if(entity.getAgentType() == AgentType.CUSTOM) {
            vo.setTool(agentToolService.getToolIds(id));
            vo.setMcp(agentMcpServerService.getMcpIds(id));
            vo.setMcpBindings(agentMcpServerService.getBindings(id));
            vo.setSkill(agentSkillPackageService.getSkillPackageIds(id));
            vo.setSubAgent(agentSubAgentService.getSubAgentIds(id));
            vo.setKnowledgeBase(agentKnowledgeBaseService.getKnowledgeIds(id));
        } else {
            vo.setAgentA2A(agentA2aService.getA2aConfigByAgentId(id));
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveAgentDefinition(AgentDefinitionVO vo) {
        AgentDefinition agentDefinition = BeanUtils.copy(vo, AgentDefinition.class);
        save(agentDefinition);
        vo.setId(agentDefinition.getId());

        saveSubItems(vo);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateAgentDefinition(AgentDefinitionVO vo) {
        AgentDefinition oldAgent = getById(vo.getId());
        updateById(BeanUtils.copy(vo, AgentDefinition.class));

        // 成立条件：禁用/启用操作
        if (vo.getAgentCode() == null) {
            List<JobInfo> agent = iJobInfoMapper.selectList(
                    new LambdaQueryWrapper<JobInfo>()
                            .eq(JobInfo::getType, "AGENT")
                            .eq(JobInfo::getBizId, vo.getId()));
            if (!agent.isEmpty() && agent.get(0).isEnabled()) {
                throw new RuntimeException("请先禁用定时任务");
            }
            if (vo.getEnabled()) {
                messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(vo.getId()));
            } else {
                AgentDefinition agentDefinition = getById(vo.getId());
                messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_UNREGISTER_CHANNEL, agentDefinition.getAgentCode());
            }

            return true;
        }

        saveSubItems(vo);

        // Agent Code 发生变化，将旧的 Agent 注销
        if (!oldAgent.getAgentCode().equals(vo.getAgentCode())) {
            messagePublisher.publish(RedisChannelTopic.AGENT_UNREGISTER_CHANNEL, oldAgent.getAgentCode());
        }

        messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(vo.getId()));
        return true;
    }

    private void saveSubItems(AgentDefinitionVO vo) {
        agentHookService.saveAgentHook(vo.getId(), vo.getHook());
        if (vo.getAgentType() == AgentType.CUSTOM) {
            agentSubAgentService.saveSubAgent(vo.getId(), vo.getSubAgent());
            agentToolService.saveAgentTool(vo.getId(), vo.getTool());
            agentMcpServerService.saveAgentMcpServer(vo.getId(), vo.getMcp(), vo.getMcpBindings());
            agentSkillPackageService.saveAgentSkillPackage(vo.getId(), vo.getSkill());
            agentKnowledgeBaseService.saveAgentKnowledge(vo.getId(), vo.getKnowledgeBase());
            if (vo.getStudioConfigId() != null) {
                agentStudioService.saveAgentStudio(vo.getId(), List.of(vo.getStudioConfigId()));
            } else {
                agentStudioService.deleteAgentStudio(List.of(vo.getId()));
            }
            if (vo.getCodeExecutionConfigId() != null) {
                agentCodeExecutionService.saveAgentCodeExecution(vo.getId(), List.of(vo.getCodeExecutionConfigId()));
            } else {
                agentCodeExecutionService.deleteAgentCodeExecution(List.of(vo.getId()));
            }
        } else {
            AgentA2A agentA2A = vo.getAgentA2A();
            agentA2A.setAgentDefinitionId(vo.getId());
            agentA2aService.saveA2aConfig(agentA2A);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAgentDefinition(List<Long> ids) {
        List<JobInfo> agent = iJobInfoMapper.selectList(
                new LambdaQueryWrapper<JobInfo>()
                        .eq(JobInfo::getType, "AGENT")
                        .in(JobInfo::getBizId, ids));
        if (!agent.isEmpty()) {
            throw new RuntimeException("请先解绑定时任务");
        }

        List<AgentDefinition> agents = listByIds(ids);

        removeByIds(ids);
        agentA2aService.deleteA2aConfig(ids);
        agentSubAgentService.deleteSubAgent(ids);
        agentHookService.deleteAgentHook(ids);
        agentToolService.deleteAgentTool(ids);
        agentMcpServerService.deleteAgentMcpServer(ids);
        agentSkillPackageService.deleteAgentSkillPackage(ids);
        agentKnowledgeBaseService.deleteAgentKnowledge(ids);
        agentStudioService.deleteAgentStudio(ids);
        agentCodeExecutionService.deleteAgentCodeExecution(ids);

        for (AgentDefinition agent_ : agents) {
            messagePublisher.publishAfterCommit(RedisChannelTopic.AGENT_UNREGISTER_CHANNEL, agent_.getAgentCode());
        }

        return Boolean.TRUE;
    }

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        ids.forEach(id -> {
            agentSubAgentService.getSubAgentIds(id).forEach(subAgentId -> {
                AgentDefinition agentDefinition = getById(subAgentId);
                if (agentDefinition != null) {
                    names.add(agentDefinition.getName());
                }
            });
        });

        return names;
    }

    @Override
    public List<String> listTags() {
        return this.lambdaQuery()
                .select(AgentDefinition::getTag)
                .isNotNull(AgentDefinition::getTag)
                .groupBy(AgentDefinition::getTag)
                .list()
                .stream()
                .map(AgentDefinition::getTag)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> allowFileType(Long id) {
        AgentDefinition agentDefinition = getById(id);
        if (agentDefinition == null || !agentDefinition.getEnabled()) {
            return List.of();
        }

        ModelConfig modelConfig = modelConfigService.getById(agentDefinition.getModelConfigId());
        if (modelConfig == null) {
            return List.of();
        }
        JsonNode modelTypeJ = modelConfig.getModelType();
        if (modelTypeJ == null) {
            return List.of();
        }

        List<String> modelType = parseModelType(modelTypeJ);
        List<String> allowImageFileType = new ArrayList<>();
        if (modelType.contains(ModelType.IMAGE.name())) {
            allowImageFileType.add(paramsAdapter.getValue("ALLOW_IMAGE_FILE_TYPE"));
        }
        if (modelType.contains(ModelType.AUDIO.name())) {
            allowImageFileType.add(paramsAdapter.getValue("ALLOW_AUDIO_FILE_TYPE"));
        }
        if (modelType.contains(ModelType.VIDEO.name())) {
            allowImageFileType.add(paramsAdapter.getValue("ALLOW_VIDEO_FILE_TYPE"));
        }

        if (allowImageFileType.isEmpty()) {
            return List.of();
        }

        String join = String.join(",", allowImageFileType);
        return List.of(join.split(","));
    }

    @Override
    public List<ToolConfig> getEnabledToolsOfAgent(Long agentId) {
        List<Long> toolIds = agentToolService.getToolIds(agentId);
        if (!toolIds.isEmpty()) {
            return toolService.list(
                    new LambdaQueryWrapper<ToolConfig>()
                            .select(ToolConfig::getId, ToolConfig::getName, ToolConfig::getToolId, ToolConfig::getDescription)
                            .eq(ToolConfig::getEnabled, true)
                            .in(ToolConfig::getId, toolIds));
        }
        return List.of();
    }

    @Override
    public List<SkillPackage> getEnabledSkillsOfAgent(Long agentId) {
        List<Long> skillPackageIds = agentSkillPackageService.getSkillPackageIds(agentId);
        if (!skillPackageIds.isEmpty()) {
            return skillPackageService.list(
                    new LambdaQueryWrapper<SkillPackage>()
                            .select(SkillPackage::getId, SkillPackage::getName, SkillPackage::getDescription)
                            .eq(SkillPackage::getEnabled, true)
                            .in(SkillPackage::getId, skillPackageIds));
        }
        return Collections.emptyList();
    }

    private List<String> parseModelType(JsonNode modelTypeJ) {
        try {
            return (List<String>)JsonUtils.parse(modelTypeJ.toString(), List.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
