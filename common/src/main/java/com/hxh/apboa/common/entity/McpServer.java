package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.McpActivationStatus;
import com.hxh.apboa.common.enums.McpMode;
import com.hxh.apboa.common.enums.McpProtocol;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * MCP 服务器配置
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
     * 超时时间
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
     * 缓存的工具目录 JSON
     */
    private String toolSchemas;

    /**
     * 健康状态
     */
    private HealthStatus healthStatus;

    /**
     * 上次健康检查时间
     */
    private LocalDateTime lastHealthCheck;

    /**
     * 激活状态
     */
    private McpActivationStatus activationStatus;

    /**
     * 激活结果说明
     */
    private String activationMessage;

    /**
     * 上次激活时间
     */
    private LocalDateTime lastActivationTime;

    /**
     * 上次工具同步时间
     */
    private LocalDateTime lastToolSyncTime;

    /**
     * 当前工具数量
     */
    private Integer toolCount;

    /**
     * 激活版本号
     */
    private Long activationRevision;

    /**
     * 当前配置摘要
     */
    private String configHash;

    /**
     * 是否需要同步
     */
    private Boolean needsSync;

    /**
     * 当前激活请求ID
     */
    private String activationRequestId;
}
