package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型类型
 *
 * @author huxuehao
 */
@Getter
@AllArgsConstructor
public enum ModelType {
    CHAT("文本"),
    IMAGE("图像"),
    VIDEO("视频"),
    AUDIO("音频");

    private final String description;
}
