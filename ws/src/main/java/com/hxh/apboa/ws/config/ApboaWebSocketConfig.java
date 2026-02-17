package com.hxh.apboa.ws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 描述：WebSocketConfig
 * 配置 WebSocket 的 endpoint
 *
 * @author huxuehao
 **/
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class ApboaWebSocketConfig implements WebSocketConfigurer {
    private final ApboaWebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/apboa").setAllowedOrigins("*");;
    }
}
