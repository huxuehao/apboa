package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具选择策略
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum ToolChoiceStrategy {
    AUTO("自动"),
    NONE("禁止调用"),
    REQUIRED("强制调用"),
    SPECIFIC("强制调用指定工具");

    private final String description;
}
