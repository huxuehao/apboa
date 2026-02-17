package com.hxh.apboa.common.wrapper;

import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.enums.ToolChoiceStrategy;
import lombok.*;

/**
 * 描述：模型配置包装类
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfigWrapper {
    /**
     * 模型提供商
     */
    private ModelProviderType provider;
    /**
     * 基础路径
     */
    private String baseUrl;

    /**
     * 模型编号
     */
    private String modelCode;

    /**
     * 模型 API Key
     */
    private String apiKey;

    /**
     * 是否支持思考
     */
    private Boolean thinking;
    /**
     * 是否支持流式
     */
    private Boolean streaming;

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
     * 是否是多智能体协同
     */
    boolean multi;

    /**
     * 工具选择策略
     */
    private ToolChoiceStrategy toolChoiceStrategy;

    /**
     * 指定工具名称
     */
    private String specificToolName;

    @Override
    public String toString() {
        return "ModelConfigWrapper{" +
                "provider=" + provider +
                ", baseUrl='" + baseUrl + '\'' +
                ", modelCode='" + modelCode + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", thinking=" + thinking +
                ", streaming=" + streaming +
                ", contextWindow=" + contextWindow +
                ", maxTokens=" + maxTokens +
                ", temperature=" + temperature +
                ", topP=" + topP +
                ", topK=" + topK +
                ", repeatPenalty=" + repeatPenalty +
                ", seed=" + seed +
                ", multi=" + multi +
                ", toolChoiceStrategy=" + toolChoiceStrategy +
                ", specificToolName='" + specificToolName + '\'' +
                '}';
    }
}
