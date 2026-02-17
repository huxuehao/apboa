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
    CHAT("对话"),
    IMAGE("生图"),
    VIDEO("视频"),
    TTS("语音"),
    EMBEDDING("向量"),
    RERANKER("排序");

    private final String description;
}
