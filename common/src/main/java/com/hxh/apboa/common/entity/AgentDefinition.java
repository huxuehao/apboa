package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.enums.ToolChoiceStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.model.StructuredOutputReminder;
import lombok.Getter;
import lombok.Setter;

/**
 * 智能体定义
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.AGENT, autoResultMap = true)
public class AgentDefinition extends BaseEntity {
    /**
     * 智能体类型
     */
    private AgentType agentType;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体代码
     */
    private String agentCode;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * 基础模型配置ID
     */
    private Long modelConfigId;

    /**
     * 模型参数覆盖
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode modelParamsOverride;

    /**
     * 工具选择策略
     */
    private ToolChoiceStrategy toolChoiceStrategy;

    /**
     * 指定工具名称
     */
    private String specificToolName;

    /**
     * 系统提示词模板ID
     */
    private Long systemPromptTemplateId;

    /**
     * 是否跟随模板变化
     */
    private Boolean followTemplate;

    /**
     * 系统提示词内容
     */
    private String systemPrompt;

    /**
     * 敏感词配置ID
     */
    private Long sensitiveWordConfigId;

    /**
     * 是否启用敏感词过滤
     */
    private Boolean sensitiveFilterEnabled;

    /**
     * React最大迭代次数
     */
    private Integer maxIterations;

    /**
     * 是否启用计划
     */
    private Boolean enablePlanning;

    /**
     * 是否显示工具调用过程
     */
    private Boolean showToolProcess;

    /**
     * 最大子任务数
     */
    private Integer maxSubtasks;

    /**
     * 计划是否需要确认
     */
    private Boolean requirePlanConfirmation;

    /**
     * 是否启用记忆
     */
    private Boolean enableMemory;

    /**
     * 是否启用记忆压缩
     */
    private Boolean enableMemoryCompression;

    /**
     * 记忆压缩配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode memoryCompressionConfig;

    /**
     * 是否启用结构化输出
     */
    private Boolean structuredOutputEnabled;

    /**
     * 结构化输出模式
     */
    private StructuredOutputReminder structuredOutputReminder;

    /**
     * 结构化输出模板
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode structuredOutputSchema;

    /**
     * 版本号
     */
    private String version;

    /**
     * 标签
     */
    private String tag;
}
