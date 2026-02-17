package com.hxh.apboa.common.dto;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

/**
 * 追加消息 DTO（正常对话或重新生成）
 *
 * @author huxuehao
 */
@Getter
@Setter
public class ChatMessageAppendDTO implements SerializableEnable {
    /**
     * 消息角色：user / assistant / system / tool
     */
    private String role;
    /**
     * 消息内容
     */
    private String content;
}
