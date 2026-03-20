package com.hxh.apboa.core.model.impl;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.core.model.IChatModel;
import com.hxh.apboa.core.model.GenerateOptionsHelper;
import com.hxh.apboa.core.model.HttpTransportHelper;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.formatter.dashscope.DashScopeMultiAgentFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

/**
 * 描述：DashScope 模型
 *
 * @author huxuehao
 **/
@Component
public class DefaultDashScopeModelI implements IChatModel {
    @Override
    public Model getModel(ModelConfigWrapper config) {
        if (config.getProvider() != getProvider()) {
            throw new IllegalArgumentException("The provider is not supported");
        }

        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelCode())
                .stream(config.getStreaming() != null && config.getStreaming())
                .enableThinking(config.getThinking() != null && config.getThinking())
                .httpTransport(HttpTransportHelper.createOkHttpTransport())
                .defaultOptions(GenerateOptionsHelper.create(config));

        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }

        if (config.isMulti()) {
            builder.formatter(new DashScopeMultiAgentFormatter());
        } else {
            builder.formatter(new DashScopeChatFormatter());
        }

        return builder.build();
    }

    @Override
    public ModelProviderType getProvider() {
        return ModelProviderType.DASH_SCOPE;
    }

    @Override
    public int order() {
        return 0;
    }
}
