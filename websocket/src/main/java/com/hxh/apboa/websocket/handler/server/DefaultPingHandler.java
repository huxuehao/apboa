package com.hxh.apboa.websocket.handler.server;

import com.hxh.apboa.websocket.config.ApboaWebSocketSessionManager;
import com.hxh.apboa.websocket.context.ApboaWebSocketSession;
import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.websocket.model.WsServerMessage;
import org.springframework.stereotype.Service;

/**
 * 描述：服务端向客户端发送PING类型的消息时的处理器。
 * 维护session健康状态
 *
 * @author huxuehao
 **/
@Service
public class DefaultPingHandler implements ServerMessageHandler {
    @Override
    public WsMessageType messageType() {
        return WsMessageType.PING;
    }

    @Override
    public void handle(ApboaWebSocketSession session, WsServerMessage msg) {
        if (msg == null) {
            return;
        }
        // 发送心跳给客户端
        ApboaWebSocketSessionManager.sendBySession(session, msg);
    }

    @Override
    public ServerMessageHandler register() {
        return this;
    }
}
