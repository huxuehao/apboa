package com.hxh.apboa.websocket.config;

import com.hxh.apboa.websocket.interceptor.WebSocketAuthInterceptor;
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
    private final ApboaWebSocketHandler apboaWebSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 添加握手拦截器进行认证
        registry.addHandler(apboaWebSocketHandler, "/ws/apboa")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
