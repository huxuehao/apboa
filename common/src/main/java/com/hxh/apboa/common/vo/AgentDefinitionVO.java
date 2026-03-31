package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.entity.AgentA2A;
import com.hxh.apboa.common.entity.JobInfo;
import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.enums.ToolChoiceStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.model.StructuredOutputReminder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体定义VO
 *
 * @author huxuehao
 */
@Data
@EqualsAndHashCode
public class AgentDefinitionVO implements SerializableEnable {
    private Long id;
    private AgentType agentType;
    private String name;
    private String agentCode;
    private String description;
    private Long modelConfigId;
    private JsonNode modelParamsOverride;
    private List<Long> skill;
    private List<Long> tool;
    private List<Long> mcp;
    private List<Long> hook;
    private List<Long> subAgent;
    private List<Long> knowledgeBase;
    private ToolChoiceStrategy toolChoiceStrategy;
    private String specificToolName;
    private Long systemPromptTemplateId;
    private Boolean followTemplate;
    private String systemPrompt;
    private Long sensitiveWordConfigId;
    private Boolean sensitiveFilterEnabled;
    private Integer maxIterations;
    private Boolean enablePlanning;
    private Integer maxSubtasks;
    private Boolean requirePlanConfirmation;
    private Boolean enableMemory;
    private Boolean enableMemoryCompression;
    private JsonNode memoryCompressionConfig;
    private Boolean structuredOutputEnabled;
    private StructuredOutputReminder structuredOutputReminder;
    private JsonNode structuredOutputSchema;
    private String version;
    private String tag;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private List<Object> used;
    private AgentA2A agentA2A;
    private JobInfo jobInfo;
    private Long studioConfigId;
    private Long codeExecutionConfigId;
}
