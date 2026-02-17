package com.hxh.apboa.agent.service.impl;

import com.hxh.apboa.agent.mapper.ChatMessageMapper;
import com.hxh.apboa.agent.service.ChatMessageService;
import com.hxh.apboa.common.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 聊天消息 Service 实现
 *
 * @author huxuehao
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Override
    public List<ChatMessage> listByIdsOrderByDepth(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .in(ChatMessage::getId, ids)
                .orderByAsc(ChatMessage::getDepth)
                .list();
    }
}
