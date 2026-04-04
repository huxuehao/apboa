package com.hxh.apboa.websocket.cluster;

import com.hxh.apboa.cluster.core.ChannelSubscriber;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.websocket.config.ApboaWebSocketSessionManager;
import com.hxh.apboa.websocket.context.ApboaWebSocketSession;
import com.hxh.apboa.websocket.model.WsServerMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 描述：Redis 消息订阅者 - 仅处理 ws:cluster:* 频道的跨节点消息
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class ClusterMessageSubscriber implements ChannelSubscriber {

    /** 允许处理的频道前缀，只接受 ws:cluster:* 消息 */
    private static final String CHANNEL_PREFIX = "ws:cluster:";
    private static final String CHANNEL_PATTERN = "ws:cluster:*";

    @Override
    public Topic getTopic() {
        return new PatternTopic(CHANNEL_PATTERN);
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            // 仅处理 ws:cluster:* 频道，拒绝非 WebSocket 集群消息
            if (!channel.startsWith(CHANNEL_PREFIX)) {
                log.warn("忽略非 WebSocket 集群频道的消息：channel={}", channel);
                return;
            }

            log.debug("接收到集群消息：channel={}", channel);

            ClusterMessage clusterMessage = JsonUtils.parse(message, ClusterMessage.class);

            // 构建消息对象
            WsServerMessage wsMessage = new WsServerMessage(
                    clusterMessage.getMessageType(),
                    clusterMessage.getContent()
            );

            // 按 clientId 精准推送
            if (clusterMessage.getTargetClientId() != null) {
                ApboaWebSocketSession session = ApboaWebSocketSession.getSessionByClientId(clusterMessage.getTargetClientId());
                if (session != null && session.writeable()) {
                    ApboaWebSocketSessionManager.sendBySession(session, wsMessage);
                    log.info("集群消息转发成功：targetClientId={}", clusterMessage.getTargetClientId());
                } else {
                    log.debug("目标客户端不在本节点：targetClientId={}", clusterMessage.getTargetClientId());
                }
            }
            // 按 userId 推送
            else if (clusterMessage.getUserId() != null) {
                List<ApboaWebSocketSession> sessions = ApboaWebSocketSession.getSessionByAccountId(clusterMessage.getUserId());
                for (ApboaWebSocketSession session : sessions) {
                    if (session != null && session.writeable()) {
                        ApboaWebSocketSessionManager.sendBySession(session, wsMessage);
                    }
                }
                log.info("集群消息转发成功：userId={}, sessionCount={}", clusterMessage.getUserId(), sessions.size());
            }
            // 全广播
            else {
                if (clusterMessage.getExcludeClientId() != null) {
                    ApboaWebSocketSessionManager.sendToOther(clusterMessage.getExcludeClientId(), wsMessage);
                } else {
                    ApboaWebSocketSessionManager.sendToAll(wsMessage);
                }
                log.info("集群广播消息转发成功：messageType={}", clusterMessage.getMessageType());
            }
        } catch (Exception e) {
            log.error("处理集群消息失败：{}", e.getMessage(), e);
        }
    }
}
