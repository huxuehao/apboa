package com.hxh.apboa.core.model.impl;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.core.model.IChatModel;
import com.hxh.apboa.core.model.GenerateOptionsHelper;
import io.agentscope.core.formatter.gemini.GeminiChatFormatter;
import io.agentscope.core.formatter.gemini.GeminiMultiAgentFormatter;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 描述：Gemini 模型
 *
 * @author huxuehao
 **/
@Component
public class DefaultGeminiModelI implements IChatModel {
    @Override
    public Model getModel(ModelConfigWrapper config) {
        if (config.getProvider() != getProvider()) {
            throw new IllegalArgumentException("The provider is not supported");
        }

        GeminiChatModel.Builder builder = GeminiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelCode())
                .vertexAI(false)
                .streamEnabled(config.getStreaming() != null && config.getStreaming())
                .defaultOptions(GenerateOptionsHelper.create(config));

        if (config.isMulti()) {
            builder.formatter(new GeminiMultiAgentFormatter());
        } else {
            builder.formatter(new GeminiChatFormatter());
        }

        return builder.build();
    }

    @Override
    public ModelProviderType getProvider() {
        return ModelProviderType.GEMINI;
    }

    @Override
    public int order() {
        return 0;
    }
}
