package com.hxh.apboa.a2a.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.AgentA2A;

import java.util.List;

/**
 * 描述：AgentA2aService
 *
 * @author huxuehao
 **/
public interface AgentA2aService extends IService<AgentA2A> {
    AgentA2A getA2aConfigByAgentId(Long agentId);
    boolean saveA2aConfig(AgentA2A agentA2A);
    boolean deleteA2aConfig(List<Long> agentIds);
}
