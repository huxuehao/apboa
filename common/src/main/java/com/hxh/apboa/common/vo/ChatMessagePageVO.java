package com.hxh.apboa.common.vo;

import com.hxh.apboa.common.config.SerializableEnable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 聊天消息分页 VO：用于滚动加载历史消息
 *
 * @author huxuehao
 */
@Getter
@Setter
public class ChatMessagePageVO implements SerializableEnable {

    /** 本页消息列表（按 depth 升序） */
    private List<ChatMessageVO> messages;

    /** 是否还有更早的历史消息 */
    private boolean hasMore;

    /** 下一页游标：本批消息中最小的 depth，传给 beforeDepth 以获取更早消息 */
    private Integer nextBeforeDepth;
}
