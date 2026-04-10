package com.hxh.apboa.common.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.consts.TableConst;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 智能体对话Key
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(TableConst.AGENT_CHAT_KEY)
@AllArgsConstructor
@NoArgsConstructor
public class AgentChatKey implements SerializableEnable {
    private String agentCode;
    private String chatKey;
}
