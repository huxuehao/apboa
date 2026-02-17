package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具类型
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum ToolType {
    BUILTIN("内置"),
    CUSTOM("自定义");

    private final String description;
}
