package com.hxh.apboa.ws.context;

import com.hxh.apboa.common.vo.WsUser;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 描述：自定义类，包装WebSocketSession
 *
 * @author huxuehao
 **/
@Getter
@Setter
public class ApboaWebSocketSession {
    private static final Map<String, ApboaWebSocketSession> CACHED = new ConcurrentHashMap<>();
    private WebSocketSession webSocketSession;
    private String clientId;
    private WsUser user;

    private long activateTime = System.currentTimeMillis();
    private long timeout;

    public ApboaWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public static Map<String, ApboaWebSocketSession> getSessionCache() {
        return CACHED;
    }

    /**
     * 判断是否可写
     */
    public boolean writeable() {
        return webSocketSession != null && webSocketSession.isOpen();
    }

    /**
     * 根据WebSocketSession获取ApboaWebSocketSession
     * @param session WebSocketSession
     */
    public static ApboaWebSocketSession from(WebSocketSession session) {
        ApboaWebSocketSession ApboaWebSocketSession = CACHED.get(session.getId());
        if (ApboaWebSocketSession == null) {
            ApboaWebSocketSession = new ApboaWebSocketSession(session);
            CACHED.put(session.getId(), ApboaWebSocketSession);
        }
        return ApboaWebSocketSession;
    }

    /**
     * 根据用户ID获取WebSocketSession集合
     * @param clientId clientId
     */
    public static ApboaWebSocketSession getSessionByClientId(String clientId) {
        if (clientId == null) {
            return null;
        }

        for (Map.Entry<String, ApboaWebSocketSession> entry : CACHED.entrySet()) {
            ApboaWebSocketSession value = entry.getValue();
            if (clientId.equals(value.getClientId())) {
                return value;
            }
        }

        return null;
    }

    /**
     * 根据账号ID获取WebSocketSession集合
     * @param accountId 账号ID
     */
    public static List<ApboaWebSocketSession> getSessionByAccountId(String accountId) {
        ArrayList<ApboaWebSocketSession> targetSessions = new ArrayList<>();
        if (accountId == null) {
            return targetSessions;
        }

        for (Map.Entry<String, ApboaWebSocketSession> entry : CACHED.entrySet()) {
            WsUser user = entry.getValue().getUser();
            if (user != null && accountId.equals(String.valueOf(user.getId()))) {
                targetSessions.add(entry.getValue());
            }
        }

        return targetSessions;
    }

    /**
     * 关闭
     */
    public void close(){
        if(this.webSocketSession != null) {
            remove(this.webSocketSession);
            try {
                if (webSocketSession.isOpen()) {
                    this.webSocketSession.close(CloseStatus.SESSION_NOT_RELIABLE);
                }
            } catch (Exception ignored) {

            }
            this.webSocketSession = null;
        }
    }

    /**
     * 移除 WebSocketSession
     * @param session WebSocketSession
     */
    private static void remove(WebSocketSession session) {
        CACHED.remove(session.getId());
    }
}
