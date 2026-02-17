package com.hxh.apboa.common.enums;

import lombok.Getter;

/**
 * 消息角色：user-用户，assistant-AI助手，system-系统
 *
 * @author huxuehao
 */
@Getter
public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    MessageRole(String value) {
        this.value = value;
    }
}
