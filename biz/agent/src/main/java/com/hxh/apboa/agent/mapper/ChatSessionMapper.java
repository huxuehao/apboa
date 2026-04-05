package com.hxh.apboa.agent.mapper;

import com.hxh.apboa.common.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 聊天会话 Mapper
 *
 * @author huxuehao
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 按日统计新增会话数
     *
     * @param agentId   智能体ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @return date -> count 映射列表
     */
    List<Map<String, Object>> countSessionsByDay(@Param("agentId") Long agentId, @Param("startDate") String startDate);

    /**
     * 按日统计活跃用户数（去重）
     *
     * @param agentId   智能体ID
     * @param startDate 开始日期（yyyy-MM-dd）
     * @return date -> count 映射列表
     */
    List<Map<String, Object>> countActiveUsersByDay(@Param("agentId") Long agentId, @Param("startDate") String startDate);
}
