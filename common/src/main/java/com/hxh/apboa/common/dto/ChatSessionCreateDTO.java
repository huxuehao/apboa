package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建会话 DTO
 *
 * @author huxuehao
 */
@Getter
@Setter
public class ChatSessionCreateDTO implements SerializableEnable {
    /**
     * 智能体ID
     */
    private Long agentId;
    /**
     * 会话标题，可选
     */
    private String title;
}
