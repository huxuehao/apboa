package com.hxh.apboa.core.model.impl;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.core.model.IChatModel;
import com.hxh.apboa.core.model.GenerateOptionsHelper;
import io.agentscope.core.formatter.anthropic.AnthropicChatFormatter;
import io.agentscope.core.formatter.anthropic.AnthropicMultiAgentFormatter;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 描述：Anthropic 模型
 *
 * @author huxuehao
 **/
@Component
public class DefaultAnthropicModelI implements IChatModel {
    @Override
    public Model getModel(ModelConfigWrapper config) {
        if (config.getProvider() != getProvider()) {
            throw new IllegalArgumentException("The provider is not supported");
        }

        AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelCode())
                .stream(config.getStreaming() != null && config.getStreaming())
                .defaultOptions(GenerateOptionsHelper.create(config));

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }

        if (config.isMulti()) {
            builder.formatter(new AnthropicMultiAgentFormatter());
        } else {
            builder.formatter(new AnthropicChatFormatter());
        }

        return builder.build();
    }

    @Override
    public ModelProviderType getProvider() {
        return ModelProviderType.ANTHROPIC;
    }

    @Override
    public int order() {
        return 0;
    }
}
