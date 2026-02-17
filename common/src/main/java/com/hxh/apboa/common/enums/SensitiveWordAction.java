package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 敏感词处理动作
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum SensitiveWordAction {
    BLOCK("阻断"),
    REPLACE("替换"),
    WARN("警告");

    private final String description;
}
