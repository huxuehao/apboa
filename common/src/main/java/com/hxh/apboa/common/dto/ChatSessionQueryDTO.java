package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

/**
 * 会话列表查询 DTO（按用户、智能体等筛选）
 *
 * @author huxuehao
 */
@Getter
@Setter
public class ChatSessionQueryDTO implements SerializableEnable {
    private Long userId;
    private Long agentId;
}
