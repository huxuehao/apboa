package com.hxh.apboa.core.model.impl;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.core.model.IChatModel;
import com.hxh.apboa.core.model.GenerateOptionsHelper;
import com.hxh.apboa.core.model.HttpTransportHelper;
import io.agentscope.core.formatter.ollama.OllamaChatFormatter;
import io.agentscope.core.formatter.ollama.OllamaMultiAgentFormatter;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ollama.OllamaOptions;
import org.springframework.stereotype.Component;

/**
 * 描述：Ollama 模型
 *
 * @author huxuehao
 **/
@Component
public class DefaultOllamaModelI implements IChatModel {
    @Override
    public Model getModel(ModelConfigWrapper config) {
        if (config.getProvider() != getProvider()) {
            throw new IllegalArgumentException("The provider is not supported");
        }

        if (config.getBaseUrl() == null) {
            throw new IllegalArgumentException("Base URL is null");
        }

        OllamaChatModel.Builder builder = OllamaChatModel.builder()
                .modelName(config.getModelCode())
                .httpTransport(HttpTransportHelper.createOkHttpTransport())
                .defaultOptions(OllamaOptions.fromGenerateOptions(GenerateOptionsHelper.create(config)));

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        } else {
            throw new IllegalArgumentException("Base URL is null");
        }

        if (config.isMulti()) {
            builder.formatter(new OllamaMultiAgentFormatter());
        } else {
            builder.formatter(new OllamaChatFormatter());
        }

        return builder.build();
    }

    @Override
    public ModelProviderType getProvider() {
        return ModelProviderType.OLLAMA;
    }

    @Override
    public int order() {
        return 0;
    }
}
