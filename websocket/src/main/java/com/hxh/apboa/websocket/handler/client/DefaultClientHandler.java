package com.hxh.apboa.websocket.handler.client;

import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.websocket.context.ApboaWebSocketSession;
import com.hxh.apboa.websocket.model.WsClientMessage;
import org.springframework.stereotype.Service;

/**
 * 描述：客户端向服务端发送PONG类型的消息时的处理器。
 * 维护session健康状态
 *
 * @author huxuehao
 **/
@Service
public class DefaultClientHandler implements ClientMessageHandler {
    @Override
    public WsMessageType messageType() {
        return WsMessageType.CLIENT;
    }

    @Override
    public void handle(ApboaWebSocketSession session, WsClientMessage msg) {
        if (msg == null) {
            return;
        }
        session.setClientId(msg.getContent().toString());
    }

    @Override
    public ClientMessageHandler register() {
        return this;
    }
}
