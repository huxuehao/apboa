package com.hxh.apboa.websocket.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 描述：集群消息 - 用于跨节点通信
 *
 * @author huxuehao
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标用户 ID
     */
    private String userId;

    /**
     * 用户编码（业务编码）
     */
    private String userCode;

    /**
     * 目标客户端 ID（可选，为空则广播）
     */
    private String targetClientId;

    /**
     * 排除的客户端 ID（广播时使用）
     */
    private String excludeClientId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息内容
     */
    private Object content;

    /**
     * 发送方节点 ID（避免循环广播）
     */
    private String sourceNodeId;

    /**
     * 时间戳
     */
    private Long timestamp = System.currentTimeMillis();
}
