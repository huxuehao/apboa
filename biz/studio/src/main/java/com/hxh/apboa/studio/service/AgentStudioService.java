package com.hxh.apboa.studio.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.AgentStudio;

import java.util.List;

/**
 * 描述：AgentStudioService
 *
 * @author huxuehao
 **/
public interface AgentStudioService extends IService<AgentStudio> {
    List<Long> getAgentIds(List<Long> studioId);
    Long getStudioIdByAgentId(Long agentId);
    Boolean insertAgentStudio(Long agentDefinitionId, List<Long> studioIds);
    Boolean deleteAgentStudio(List<Long> agentIds);
    Boolean saveAgentStudio(Long agentDefinitionId, List<Long> studioIds);
}
