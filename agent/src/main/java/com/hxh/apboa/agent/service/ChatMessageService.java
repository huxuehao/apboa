package com.hxh.apboa.agent.service;

import com.hxh.apboa.common.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 聊天消息 Service
 *
 * @author huxuehao
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 根据路径上的 id 列表查询消息，按 depth 升序（回显当前完整对话，O(depth)）
     *
     * @param ids 路径拆分出的消息 id 列表
     * @return 按深度排序的消息列表
     */
    List<ChatMessage> listByIdsOrderByDepth(List<Long> ids);
}
