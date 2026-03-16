package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MCP服务器配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.MCP, autoResultMap = true)
public class McpServer extends BaseEntity {

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 协议类型
     */
    private McpProtocol protocol;

    /**
     * 运行模式
     */
    private McpMode mode;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 协议配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode protocolConfig;

    /**
     * 描述
     */
    private String description;

    /**
     * 健康状态
     */
    private HealthStatus healthStatus;

    /**
     * 最后健康检查时间
     */
    private LocalDateTime lastHealthCheck;
}
