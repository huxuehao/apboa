package com.hxh.apboa.agent.mapper;

import com.hxh.apboa.common.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper
 *
 * @author huxuehao
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
