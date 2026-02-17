package com.hxh.apboa.ws.handler;


import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.ws.handler.client.ClientMessageHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：websocket 消息处理器适配器
 *
 * @author huxuehao
 **/
public class ClientMessageHandlerAdapter {
    private static final Map<WsMessageType, ClientMessageHandler> CACHED = new ConcurrentHashMap<>();


    public static void register(ClientMessageHandler handler) {
        CACHED.put(handler.messageType(), handler);
    }

    public static ClientMessageHandler getHandler(WsMessageType type) {
        return CACHED.get(type);
    }
}
