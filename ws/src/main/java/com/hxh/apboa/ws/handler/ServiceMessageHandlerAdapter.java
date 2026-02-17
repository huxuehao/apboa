package com.hxh.apboa.ws.handler;

import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.ws.handler.server.ServerMessageHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：
 *
 * @author huxuehao
 **/
public class ServiceMessageHandlerAdapter {
    private static final Map<WsMessageType, ServerMessageHandler> CACHED = new ConcurrentHashMap<>();


    public static void register(ServerMessageHandler handler) {
        CACHED.put(handler.messageType(), handler);
    }

    public static ServerMessageHandler getHandler(WsMessageType type) {
        return CACHED.get(type);
    }
}
