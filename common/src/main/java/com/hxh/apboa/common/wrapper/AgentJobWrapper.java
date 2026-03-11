package com.hxh.apboa.common.wrapper;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述：智能体任务包装类
 *
 * @author huxuehao
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentJobWrapper implements SerializableEnable {
    private String agentId;
    private String input;
}
