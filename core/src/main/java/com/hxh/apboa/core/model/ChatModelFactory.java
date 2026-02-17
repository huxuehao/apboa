package com.hxh.apboa.core.model;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.hxh.apboa.common.wrapper.ModelWrapper;
import com.hxh.apboa.model.service.ModelConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.model.Model;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 描述： 聊天模型工厂
 *
 * @author huxuehao
 **/
@Component
public class ChatModelFactory {
    private static final Map<ModelProviderType, IChatModel> MODEL_MAP = new ConcurrentHashMap<>();

    private final ModelConfigService modelConfigService;

    public ChatModelFactory(List<IChatModel> IChatModels, ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
        IChatModels.stream()
                .collect(Collectors.groupingBy(IChatModel::getProvider))
                .forEach((provider, models) -> {
                    // 降序
                    models.sort((o1, o2) -> o2.order() - o1.order());
                    // 获取优先级最高的实现
                    MODEL_MAP.put(provider, models.getFirst());
                });
    }

    /**
     * 获取模型
     *
     * @param agentDefinition ModelConfigWrapper
     * @return 模型
     */
    public Model getModel(AgentDefinition agentDefinition) {
        return getModel(agentDefinition, false);
    }

    /**
     * 获取模型
     *
     * @param agentDefinition ModelConfigWrapper
     * @return 模型
     */
    public Model getModel(AgentDefinition agentDefinition, boolean multi) {
        ModelConfigWrapper configWrapper = ModelConfigWrapper.builder().build();

        // 获取agent中配置的参数
        JsonNode modelParamsOverride = agentDefinition.getModelParamsOverride();
        if (modelParamsOverride != null && !modelParamsOverride.isEmpty()) {
            configWrapper.setTemperature(modelParamsOverride.get("temperature").asDouble());
            configWrapper.setTopP(modelParamsOverride.get("topP").asDouble());
            configWrapper.setTopK(modelParamsOverride.get("topK").asInt());
            configWrapper.setMaxTokens(modelParamsOverride.get("maxTokens").asInt());
            configWrapper.setRepeatPenalty(modelParamsOverride.get("repeatPenalty").asDouble());
            configWrapper.setStreaming(modelParamsOverride.get("streaming").asBoolean());
            configWrapper.setSeed(modelParamsOverride.get("seed").asLong());
        }

        ModelWrapper config = modelConfigService.getModelWrapperById(agentDefinition.getModelConfigId());
        // 填充模型配置
        config.getConfig().fillModelConfigWrapper(configWrapper);
        // 填充供应商配置
        config.getProvider().fillModelConfigWrapper(configWrapper);
        configWrapper.setMulti(multi);
        configWrapper.setToolChoiceStrategy(agentDefinition.getToolChoiceStrategy());
        configWrapper.setSpecificToolName(agentDefinition.getSpecificToolName());

        IChatModel IChatModel = MODEL_MAP.get(configWrapper.getProvider());
        if (IChatModel == null) {
            throw new RuntimeException("No chat model found for provider " + configWrapper.getProvider());
        }

        return IChatModel.getModel(configWrapper);
    }
}
