package com.hxh.apboa.core.model;

import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolChoice;

/**
 * 描述：GenerateOptions 生成器
 *
 * @author huxuehao
 **/
public class GenerateOptionsHelper {
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final double DEFAULT_TOP_P = 0.9;
    private static final int DEFAULT_TOP_K = 40;
    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final long DEFAULT_SEED = 42L;
    private static final double DEFAULT_REPEAT_PENALTY = 0.5;
    private static final int THINKING_BUDGET = 5000;

    public static GenerateOptions create(ModelConfigWrapper config) {
        GenerateOptions.Builder builder = GenerateOptions.builder()
                .temperature(orDefault(config.getTemperature(), DEFAULT_TEMPERATURE))
                .topP(orDefault(config.getTopP(), DEFAULT_TOP_P))
                .topK(orDefault(config.getTopK(), DEFAULT_TOP_K))
                .maxTokens(orDefault(config.getMaxTokens(), DEFAULT_MAX_TOKENS))
                .seed(orDefault(config.getSeed(), DEFAULT_SEED))
                .stream(config.getStreaming() != null && config.getStreaming())
                .presencePenalty(orDefault(config.getRepeatPenalty(), DEFAULT_REPEAT_PENALTY));

        // 工具选择策略
        switch (config.getToolChoiceStrategy()) {
            case NONE -> builder.toolChoice(new ToolChoice.None());
            case REQUIRED -> builder.toolChoice(new ToolChoice.Required());
            case SPECIFIC -> builder.toolChoice(new ToolChoice.Specific(config.getSpecificToolName()));
            default -> builder.toolChoice(new ToolChoice.Auto());
        }

        if (config.getThinking()) {
            builder.thinkingBudget(THINKING_BUDGET);
        }

        return builder.build();
    }

    private static <T> T orDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
