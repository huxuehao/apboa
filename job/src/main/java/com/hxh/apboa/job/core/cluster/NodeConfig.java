package com.hxh.apboa.job.core.cluster;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.util.CryptoUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 描述：集群节点配置
 * 管理当前节点的唯一标识和基本信息
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class NodeConfig {

    /**
     * 节点唯一标识
     */
    @Getter
    private String nodeId;

    /**
     * 节点名称
     */
    @Getter
    private String nodeName;

    /**
     * 节点IP地址
     */
    @Getter
    private String nodeIp;

    /**
     * 节点启动时间
     */
    @Getter
    private long startTime;

    @PostConstruct
    public void init() {
        this.nodeId = SysConst.CURRENT_NODE_ID;
        this.nodeName = generateNodeName();
        this.nodeIp = getLocalIp();
        this.startTime = System.currentTimeMillis();
        log.info("集群节点初始化完成 - nodeId: {}, nodeName: {}, nodeIp: {}", nodeId, nodeName, nodeIp);
    }

    /**
     * 生成节点名称
     *
     * @return 节点名称
     */
    private String generateNodeName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return hostName + "-" + System.currentTimeMillis() % 10000;
        } catch (UnknownHostException e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }

    /**
     * 获取本地IP地址
     *
     * @return IP地址
     */
    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
