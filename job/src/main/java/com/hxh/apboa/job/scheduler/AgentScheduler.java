package com.hxh.apboa.job.scheduler;

import com.hxh.apboa.common.wrapper.AgentJobWrapper;
import com.hxh.apboa.core.agent.IAgentFactory;
import com.hxh.apboa.job.consts.JobConst;
import com.hxh.apboa.job.core.job.QuartzJob;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;

/**
 * 描述：智能体任务
 *
 * @author huxuehao
 **/
@Slf4j
public class AgentScheduler extends QuartzJob {
    @Override
    public Object doJob(JobExecutionContext context) {
        AgentJobWrapper wrapper = getDataMap(JobConst.DATA_MAP_KEY, AgentJobWrapper.class);
        if (wrapper == null) {
            return false;
        }

        String agentId = wrapper.getAgentId();
        if (agentId == null || agentId.trim().isEmpty()) {
            return false;
        }

        try {
            IAgentFactory agentFactory = getBean(IAgentFactory.class);
            Agent agent = agentFactory.getAgent(Long.valueOf(agentId.trim()));
            agent.call(
                    Msg.builder()
                            .textContent(wrapper.getInput())
                            .build())
                    .block();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
