package com.hxh.apboa.websocket.model;

import com.hxh.apboa.common.util.JsonUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述：WebSocket 消息包装类，用于服务器发送消息给客户端
 *
 * @author huxuehao
 **/
@Getter
@Setter
@Builder
public class WsServerMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String type;
    private Object content;

    public WsServerMessage(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public static WsServerMessage build(String type, Object content) {
        return new WsServerMessage(type, content);
    }

    public String toJson() {
        return JsonUtils.toJsonStr(this);
    }
}
