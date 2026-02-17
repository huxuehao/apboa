package com.hxh.apboa.common.enums;

/**
 * 描述：WebSocket 消息类型
 *
 * @author huxuehao
 **/
public enum WsMessageType {
    /**
     * 服务端向客户端发送客户端在线验证
     */
    PING,
    /**
     * 户端向服务端响应在连接正常的
     */
    PONG,
    /**
     * 账号
     */
    USER,
    /**
     * 账号角色变化
     */
    ACCOUNT_ROLE_CHANGE
}
