package com.hxh.apboa.agent.service.impl;

import com.hxh.apboa.agent.mapper.AgentDefinitionMapper;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.agent.service.AgentSubAgentService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.hxh.apboa.common.event.AgentReRegisterEvent;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.hxh.apboa.hook.service.AgentHookService;
import com.hxh.apboa.knowledge.service.AgentKnowledgeBaseService;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.tool.service.AgentToolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final ApplicationEventPublisher publisher;

    @Override
    public AgentDefinitionVO agentDefinitionDetail(Long id) {
        AgentDefinition entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("AgentDefinition not found for id: " + id);
        }

        AgentDefinitionVO vo = BeanUtils.copy(entity, AgentDefinitionVO.class);
        vo.setHook(hookService.getHookIds(id));
        vo.setTool(toolService.getToolIds(id));
        vo.setMcp(mcpServerService.getMcpIds(id));
        vo.setSkill(skillPackageService.getSkillPackageIds(id));
        vo.setSubAgent(subAgentService.getSubAgentIds(id));
        vo.setKnowledgeBase(agentKnowledgeBaseService.getKnowledgeIds(id));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveAgentDefinition(AgentDefinitionVO vo) {
        AgentDefinition agentDefinition = BeanUtils.copy(vo, AgentDefinition.class);
        save(agentDefinition);
        vo.setId(agentDefinition.getId());

        return saveSubItems(vo);
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

    private Boolean saveSubItems(AgentDefinitionVO vo) {
        subAgentService.saveSubAgent(vo.getId(), vo.getSubAgent());
        hookService.saveAgentHook(vo.getId(), vo.getHook());
        toolService.saveAgentTool(vo.getId(), vo.getTool());
        mcpServerService.saveAgentMcpServer(vo.getId(), vo.getMcp());
        skillPackageService.saveAgentSkillPackage(vo.getId(), vo.getSkill());
        agentKnowledgeBaseService.saveAgentKnowledge(vo.getId(), vo.getKnowledgeBase());

        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteAgentDefinition(List<Long> ids) {
        removeByIds(ids);
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
}
