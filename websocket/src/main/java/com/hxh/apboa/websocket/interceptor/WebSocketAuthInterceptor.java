package com.hxh.apboa.websocket.interceptor;

import com.hxh.apboa.common.UserDetail;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.exception.NotAuthException;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.util.RedisUtils;
import com.hxh.apboa.common.util.TokenUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 描述：WebSocket 握手拦截器 - 实现连接认证
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final RedisUtils redisUtils;
    /**
     * 握手前执行
     *
     * @param request    HTTP 请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 属性集合（可用于传递到 WebSocketSession）
     * @return true 允许握手，false 拒绝握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest) {

            // 验证 token
            String token = TokenUtils.getToken();
            Claims claims = TokenUtils.parseAndValidateToken(token);
            if (redisUtils.get(SysConst.LOGIN_USER_KEY + token) == null) {
                throw new NotAuthException("用户未登录");
            }

            // 将用户信息存入 attributes，会在 WebSocketSession 建立后传递
            attributes.put(SysConst.USER_DETAIL, JsonUtils.parse(claims.getSubject(), UserDetail.class));
            return true;
        }

        return false;
    }

    /**
     * 握手后执行
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后可以做一些日志记录或其他操作
        if (exception != null) {
            log.error("WebSocket 握手异常：{}", exception.getMessage(), exception);
        }
    }
}
