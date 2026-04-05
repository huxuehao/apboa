package com.hxh.apboa.common.config.auth;

import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.message.SkSyncMessage;
import com.hxh.apboa.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 描述：SK ID同步消息发布器
 * 用于向Redis发布SK ID的变更消息，实现多节点同步
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class SkIdSyncPublisher {

    /**
     * Redis频道名称，用于SK ID同步
     */
    private final MessagePublisher messagePublisher;

    /**
     * 发布添加SK ID的消息
     *
     * @param skId SK ID
     */
    public void publishAdd(Long skId) {
        try {
            SkSyncMessage message = SkSyncMessage.createAddMessage(SysConst.CURRENT_NODE_ID, skId);
            String jsonMessage = JsonUtils.toJsonStr(message);
            if (jsonMessage != null) {
                messagePublisher.publish(RedisChannelTopic.SK_SYNC_CHANNEL, jsonMessage);
            }
            log.debug("发布SK ID添加消息成功 - skId: {}, node: {}", skId, SysConst.CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("发布SK ID添加消息失败 - skId: {}", skId, e);
        }
    }

    /**
     * 发布移除SK ID的消息
     *
     * @param skIds SK ID列表
     */
    public void publishRemove(List<Long> skIds) {
        try {
            SkSyncMessage message = SkSyncMessage.createRemoveMessage(SysConst.CURRENT_NODE_ID, skIds);
            String jsonMessage = JsonUtils.toJsonStr(message);
            if (jsonMessage != null) {
                messagePublisher.publish(RedisChannelTopic.SK_SYNC_CHANNEL, jsonMessage);
            }
            log.debug("发布SK ID移除消息成功 - skIds: {}, node: {}", skIds, SysConst.CURRENT_NODE_ID);
        } catch (Exception e) {
            log.error("发布SK ID移除消息失败 - skIds: {}", skIds, e);
        }
    }
}
