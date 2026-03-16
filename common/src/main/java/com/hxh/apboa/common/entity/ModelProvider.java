package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.AuthType;
import com.hxh.apboa.common.enums.ModelProviderType;
import com.hxh.apboa.common.wrapper.ModelConfigWrapper;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

/**
 * 模型提供商
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.PROVIDER, autoResultMap = true)
public class ModelProvider extends BaseEntity {

    /**
     * 提供商类型: DashScope, OpenAI, Anthropic, Gemini, Ollama
     */
    private ModelProviderType type;

    /**
     * 提供商名称
     */
    private String name;

    /**
     * 提供商描述
     */
    private String description;

    /**
     * 基础URL
     */
    private String baseUrl;

    /**
     * 认证类型: 直接配置/环境变量
     */
    private AuthType authType;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 环境变量名（当auth_type=ENV时使用）
     */
    private String envVarName;

    /**
     * 提供商特定配置元数据
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode configMeta;

    public void fillModelConfigWrapper(ModelConfigWrapper configWrapper) {
        configWrapper.setProvider(this.type);
        configWrapper.setBaseUrl(this.baseUrl);

        if (authType == AuthType.CONFIG) {
            configWrapper.setApiKey(this.apiKey);
        } else {
            String apikeyEnvVal = System.getenv(this.envVarName);
            if (apikeyEnvVal == null) {
                throw new RuntimeException("env var " + this.envVarName + " is not set");
            }

            configWrapper.setApiKey(apikeyEnvVal);
        }


//        return ModelConfigWrapper.builder()
////                .thing(this.isThing())
//                .build();
    }
}
