package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.SerializableEnable;
import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.enums.A2aType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 描述：Agent和A2A的关联关系
 *
 * @author huxuehao
 **/
@Getter
@Setter
@NoArgsConstructor
@TableName(value = "agent_a2a", autoResultMap = true)
public class AgentA2A implements SerializableEnable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long agentDefinitionId;
    private A2aType a2aType;
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode a2aConfig;

    public AgentA2A(Long agentDefinitionId, A2aType a2aType, JsonNode a2aConfig) {
        this.agentDefinitionId = agentDefinitionId;
        this.a2aType = a2aType;
        this.a2aConfig = a2aConfig;
    }
}
