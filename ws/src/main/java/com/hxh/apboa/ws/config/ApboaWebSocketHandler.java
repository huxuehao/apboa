package com.hxh.apboa.ws.config;

import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.ws.context.ApboaWebSocketSession;
import com.hxh.apboa.ws.handler.ClientMessageHandlerAdapter;
import com.hxh.apboa.ws.handler.client.ClientMessageHandler;
import com.hxh.apboa.ws.model.WsClientMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 描述：WebSocket 处理器
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class ApboaWebSocketHandler extends TextWebSocketHandler {

    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将 WebSocketSession 包装成 ApboaWebSocketSession 并进行缓存
        ApboaWebSocketSession.from(session);
    }

    /**
     * 链接断开时
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ApboaWebSocketSessionManager.remove(ApboaWebSocketSession.from(session));
    }

    /**
     * 处理客户端的消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 接收到消息，进行处理
        String payload = message.getPayload();
        try {
            // 将消息负载转成对象
            WsClientMessage messageWrap = JsonUtils.parse(payload, WsClientMessage.class);
            // 获取消息处理器
            ClientMessageHandler handler = ClientMessageHandlerAdapter.getHandler(messageWrap.getType());
            if (handler != null) {
                handler.handle(ApboaWebSocketSession.from(session), messageWrap);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
