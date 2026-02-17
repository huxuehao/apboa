package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 多智能体模式
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum MultiAgentMode {
    PIPELINE("管道"),
    MSG_HUB("MsgHub"),
    AGENT_AS_TOOL("Agent as Tool"),
    DEBATE("多智能体辩论");

    private final String description;
}
