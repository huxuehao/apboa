package com.hxh.apboa.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 描述：模型提供商类型
 *
 * @author huxuehao
 **/
@Getter
@AllArgsConstructor
public enum ModelProviderType {
    DASH_SCOPE("DashScope"),
    OPEN_AI("OpenAI"),
    ANTHROPIC("Anthropic"),
    GEMINI("Gemini"),
    OLLAMA("Ollama");

    private final String description;
}
