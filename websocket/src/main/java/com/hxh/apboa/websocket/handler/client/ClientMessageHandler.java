package com.hxh.apboa.websocket.handler.client;

import com.hxh.apboa.websocket.context.ApboaWebSocketSession;
import com.hxh.apboa.common.enums.WsMessageType;
import com.hxh.apboa.websocket.handler.ClientMessageHandlerAdapter;
import com.hxh.apboa.websocket.model.WsClientMessage;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 描述：客户端 websocket 消息处理器
 *
 * @author huxuehao
 **/
public interface ClientMessageHandler extends SmartInitializingSingleton {
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
    void handle(ApboaWebSocketSession session, WsClientMessage msg);

    /**
     * 注册实现类的Handler
     */
    ClientMessageHandler register();


    @Override
    default void afterSingletonsInstantiated() {
        ClientMessageHandlerAdapter.register(register());
    }
}
