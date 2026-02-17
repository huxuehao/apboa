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
 * 聊天会话表，存储用户与智能体的对话会话信息。current_message_id 表示当前用户正在查看/继续对话的叶子节点（光标）。
 *
 * @author huxuehao
 */
@Getter
@Setter
@TableName("chat_session")
public class ChatSession implements SerializableEnable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID，关联用户表
     */
    private Long userId;

    /**
     * 智能体ID，关联智能体表
     */
    private Long agentId;

    /**
     * 当前消息ID，指向当前查看/继续对话的叶子节点
     */
    private Long currentMessageId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 是否置顶，默认 false
     */
    private Boolean isPinned = false;

    /**
     * 置顶时间
     */
    private LocalDateTime pinTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
