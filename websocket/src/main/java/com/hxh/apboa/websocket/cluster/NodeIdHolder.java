package com.hxh.apboa.websocket.cluster;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 描述：节点 ID 持有者 - 生成并持有当前节点的唯一标识
 * <p>
 * 节点 ID 格式：{appName}:{ip}:{port}
 * 用于集群消息去重，避免 Redis Pub/Sub 广播回发布方时重复推送消息
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class NodeIdHolder {

    @Value("${spring.application.name:qianmo-ws}")
    private String appName;

    @Value("${server.port:8080}")
    private int port;

    private String nodeId;

    @PostConstruct
    public void init() {
        String ip;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "unknown";
            log.warn("获取本机 IP 失败，使用默认值：{}", ip);
        }
        this.nodeId = appName + ":" + ip + ":" + port;
        log.info("当前节点 ID：{}", this.nodeId);
    }

    /**
     * 获取当前节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }
}
