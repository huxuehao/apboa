package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.util.ExtendConfigHelper;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 模型配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.MODEL, autoResultMap = true)
public class ModelConfig extends BaseEntity {

    /**
     * 提供商ID
     */
    private Long providerId;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型编号/标识符
     */
    private String modelId;

    /**
     * 模型类型
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode modelType;

    /**
     * 模型描述
     */
    private String description;

    /**
     * 是否支持流式
     */
    private Boolean streaming;
    /**
     * 是否支持思考
     */
    private Boolean thinking;

    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;

    /**
     * 最大输出token数
     */
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 核采样参数
     */
    private Double topP;

    /**
     * Top-K采样
     */
    private Integer topK;

    /**
     * 重复惩罚
     */
    private Double repeatPenalty;

    /**
     * 随机种子
     */
    private Long seed;

    /**
     * 扩展配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode extendConfig;

    /**
     * 连接性检测状态: NOT_CHECKED/CHECKING/CONNECTED/FAILED
     */
    private String connectivityStatus;

    /**
     * 连接性检测消息
     */
    private String connectivityMessage;

    /**
     * 最后连接性检测时间
     */
    private LocalDateTime lastConnectivityCheck;

    public void fillModelConfigWrapper(ModelConfigWrapper configWrapper) {
        configWrapper.setModelCode(this.modelId);
        configWrapper.setStreaming(configWrapper.getStreaming() == null ? this.streaming: configWrapper.getStreaming());
        configWrapper.setThinking(configWrapper.getThinking() == null ? this.thinking: configWrapper.getThinking());
        configWrapper.setContextWindow(configWrapper.getContextWindow() == null ? this.contextWindow: configWrapper.getContextWindow());
        configWrapper.setMaxTokens(configWrapper.getMaxTokens() == null ? this.maxTokens: configWrapper.getMaxTokens());
        configWrapper.setTemperature(configWrapper.getTemperature() == null ? this.temperature: configWrapper.getTemperature());
        configWrapper.setTopP(configWrapper.getTopP() == null ? this.topP: configWrapper.getTopP());
        configWrapper.setTopK(configWrapper.getTopK() == null ? this.topK: configWrapper.getTopK());
        configWrapper.setRepeatPenalty(configWrapper.getRepeatPenalty() == null ? this.repeatPenalty: configWrapper.getRepeatPenalty());
        configWrapper.setSeed(configWrapper.getSeed() == null ? this.seed: configWrapper.getSeed());
        ExtendConfigHelper.fillIfAbsent(configWrapper, this.extendConfig);
    }
}
