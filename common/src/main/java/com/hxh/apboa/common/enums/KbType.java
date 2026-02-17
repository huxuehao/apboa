package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 知识库类型
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum KbType {
    BAILIAN("百炼"),
    DIFY("Dify"),
    RAGFLOW("Ragflow");

    private final String description;
}
