package com.hxh.apboa.ws.message;

import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.common.message.AccountRoleChangeMessage;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.ws.context.ApboaWebSocketSession;
import com.hxh.apboa.ws.handler.server.ServerMessageHandler;
import com.hxh.apboa.ws.model.WsServerMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

/**
 * 描述：用户的账号角色发生变化时，通知客户端
 *
 * @author huxuehao
 **/
@Slf4j
@Service
public class AccountRoleChangeHandler implements ServerMessageHandler {
    @Override
    public WsMessageType messageType() {
        return WsMessageType.ACCOUNT_ROLE_CHANGE;
    }

    @Override
    public void handle(ApboaWebSocketSession session, WsServerMessage msg) {
        if (msg == null) {
            return;
        }
        if (msg.getContent() instanceof AccountRoleChangeMessage) {
            try {
                session.getWebSocketSession().sendMessage(new TextMessage(JsonUtils.toJsonStr(msg)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    @Override
    public ServerMessageHandler register() {
        return this;
    }
}
