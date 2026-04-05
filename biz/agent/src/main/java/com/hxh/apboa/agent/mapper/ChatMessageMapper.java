package com.hxh.apboa.agent.mapper;

import com.hxh.apboa.common.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息 Mapper
 *
 * @author huxuehao
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 按日统计消息数（关联某智能体的会话）
     *
     * @param agentId   智能体ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @return date -> count 映射列表
     */
    List<Map<String, Object>> countMessagesByDay(@Param("agentId") Long agentId, @Param("startDate") String startDate);

    /**
     * 按日统计平均对话轮次（每会话平均消息数）
     *
     * @param agentId   智能体ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @return date -> avg_rounds 映射列表
     */
    List<Map<String, Object>> avgRoundsByDay(@Param("agentId") Long agentId, @Param("startDate") String startDate);
}
