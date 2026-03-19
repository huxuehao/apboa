package com.hxh.apboa.common.entity;

import com.hxh.apboa.common.config.mybatis.JsonNodeTypeHandler;
import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.enums.HealthStatus;
import com.hxh.apboa.common.enums.KbType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.rag.RAGMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 知识库配置
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName(value = TableConst.KNOWLEDGE, autoResultMap = true)
public class KnowledgeBaseConfig extends BaseEntity {

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库类型
     */
    private KbType kbType;

    /**
     * 集成模式
     */
    private RAGMode ragMode;

    /**
     * 描述
     */
    private String description;

    /**
     * 连接配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode connectionConfig;

    /**
     * 端点配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode endpointConfig;

    /**
     * 检索配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode retrievalConfig;

    /**
     * 重排序配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode rerankingConfig;

    /**
     * 查询重写配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode queryRewriteConfig;

    /**
     * 元数据过滤
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode metadataFilters;

    /**
     * HTTP配置
     */
    @TableField(typeHandler = JsonNodeTypeHandler.class)
    private JsonNode httpConfig;

    /**
     * 健康状态
     */
    private HealthStatus healthStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;
}
