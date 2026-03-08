package com.hxh.apboa.websocket.handler.server;

import com.hxh.apboa.websocket.context.ApboaWebSocketSession;
import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.websocket.handler.ServiceMessageHandlerAdapter;
import com.hxh.apboa.websocket.model.WsServerMessage;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 描述：服务端 websocket 消息处理器
 *
 * @author huxuehao
 **/
public interface ServerMessageHandler extends SmartInitializingSingleton  {
    /**
     * 可以处理的消息类型
     */
    WsMessageType messageType();

    /**
     * 处理消息的执行方法
     *
     * @param session ApboaWebSocketSession
     * @param msg     消息
     */
    void handle(ApboaWebSocketSession session, WsServerMessage msg);

    /**
     * 注册实现类的Handler
     */
    ServerMessageHandler register();


    @Override
    default void afterSingletonsInstantiated() {
        ServiceMessageHandlerAdapter.register(register());
    }
}
