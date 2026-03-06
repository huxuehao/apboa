package com.hxh.apboa.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.a2a.service.AgentA2aService;
import com.hxh.apboa.agent.mapper.AgentDefinitionMapper;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.agent.service.AgentSubAgentService;
import com.hxh.apboa.common.entity.AgentA2A;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.enums.ModelType;
import com.hxh.apboa.common.event.AgentReRegisterEvent;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.hxh.apboa.hook.service.AgentHookService;
import com.hxh.apboa.knowledge.service.AgentKnowledgeBaseService;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.model.service.ModelConfigService;
import com.hxh.apboa.params.core.ParamsAdapter;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.tool.service.AgentToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AgentHookService hookService;
    private final AgentToolService toolService;
    private final AgentMcpServerService mcpServerService;
    private final AgentSkillPackageService skillPackageService;
    private final AgentSubAgentService subAgentService;
    private final AgentKnowledgeBaseService agentKnowledgeBaseService;
    private final ModelConfigService modelConfigService;
    private final ApplicationEventPublisher publisher;
    private final ParamsAdapter paramsAdapter;
    private final AgentA2aService agentA2aService;

    @Override
    public AgentDefinitionVO agentDefinitionDetail(Long id) {
        AgentDefinition entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("AgentDefinition not found for id: " + id);
        }

        AgentDefinitionVO vo = BeanUtils.copy(entity, AgentDefinitionVO.class);

        vo.setHook(hookService.getHookIds(id));
        if(entity.getAgentType() == AgentType.CUSTOM) {
            vo.setTool(toolService.getToolIds(id));
            vo.setMcp(mcpServerService.getMcpIds(id));
            vo.setSkill(skillPackageService.getSkillPackageIds(id));
            vo.setSubAgent(subAgentService.getSubAgentIds(id));
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

        publisher.publishEvent(new AgentReRegisterEvent(vo.getId()));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateAgentDefinition(AgentDefinitionVO vo) {
        updateById(BeanUtils.copy(vo, AgentDefinition.class));

        if (vo.getAgentCode() == null) {
            publisher.publishEvent(new AgentReRegisterEvent(vo.getId()));
            return true;
        }

        saveSubItems(vo);

        publisher.publishEvent(new AgentReRegisterEvent(vo.getId()));
        return true;
    }

    private void saveSubItems(AgentDefinitionVO vo) {
        hookService.saveAgentHook(vo.getId(), vo.getHook());
        if (vo.getAgentType() == AgentType.CUSTOM) {
            subAgentService.saveSubAgent(vo.getId(), vo.getSubAgent());
            toolService.saveAgentTool(vo.getId(), vo.getTool());
            mcpServerService.saveAgentMcpServer(vo.getId(), vo.getMcp());
            skillPackageService.saveAgentSkillPackage(vo.getId(), vo.getSkill());
            agentKnowledgeBaseService.saveAgentKnowledge(vo.getId(), vo.getKnowledgeBase());
        } else {
            AgentA2A agentA2A = vo.getAgentA2A();
            agentA2A.setAgentDefinitionId(vo.getId());
            agentA2aService.saveA2aConfig(agentA2A);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAgentDefinition(List<Long> ids) {
        removeByIds(ids);
        agentA2aService.deleteA2aConfig(ids);
        subAgentService.deleteSubAgent(ids);
        hookService.deleteAgentHook(ids);
        toolService.deleteAgentTool(ids);
        mcpServerService.deleteAgentMcpServer(ids);
        skillPackageService.deleteAgentSkillPackage(ids);
        agentKnowledgeBaseService.deleteAgentKnowledge(ids);

        return Boolean.TRUE;
    }

    @Override
    public List<Object> usedWithAgent(List<Long> ids) {
        List<Object> names = new ArrayList<>();
        ids.forEach(id -> {
            subAgentService.getSubAgentIds(id).forEach(subAgentId -> {
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

    private List<String> parseModelType(JsonNode modelTypeJ) {
        try {
            return (List<String>)JsonUtils.parse(modelTypeJ.toString(), List.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
