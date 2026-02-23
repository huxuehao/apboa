package com.hxh.apboa.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 聊天消息表，存储会话中的所有消息，支持树状结构。path 为物化路径，用于 O(depth) 回显与上下文构建。
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("chat_message")
public class ChatMessage implements SerializableEnable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 会话ID，关联 chat_session 表
     */
    private Long sessionId;

    /**
     * 消息角色：user / assistant / system / tool
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 父消息ID，NULL 表示根消息
     */
    private Integer parentId;

    /**
     * 消息路径，格式如 /1/2/3/，用于快速查询消息链
     */
    private String path;

    /**
     * 消息深度，根消息为 0
     */
    private Integer depth;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
