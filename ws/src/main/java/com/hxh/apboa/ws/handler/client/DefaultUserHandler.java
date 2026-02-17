package com.hxh.apboa.ws.handler.client;

import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.WsUser;
import com.hxh.apboa.ws.context.ApboaWebSocketSession;
import com.hxh.apboa.ws.model.WsClientMessage;
import org.springframework.stereotype.Service;

/**
 * 描述：客户端向服务端发送User类型的消息时的处理器。
 * 维护session健康状态
 *
 * @author huxuehao
 **/
@Service
public class DefaultUserHandler implements ClientMessageHandler {
    @Override
    public WsMessageType messageType() {
        return WsMessageType.USER;
    }

    @Override
    public void handle(ApboaWebSocketSession session, WsClientMessage msg) {
        if (msg == null) return;

        WsUser user = JsonUtils.parse(msg.getContent().toString(), WsUser.class);
        session.setUser(user);
        session.setClientId(user.getClientId());
    }

    @Override
    public ClientMessageHandler register() {
        return this;
    }
}
