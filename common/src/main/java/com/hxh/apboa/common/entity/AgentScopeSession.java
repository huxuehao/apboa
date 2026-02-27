package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 描述：AgentscopeSession
 *
 * @author huxuehao
 **/
@Getter
@Setter
@TableName("agentscope_sessions")
@AllArgsConstructor
@NoArgsConstructor
public class AgentScopeSession implements SerializableEnable {
    @TableId
    private String sessionId;
    private String stateKey;
    private Integer itemIndex;
    private String stateData;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
