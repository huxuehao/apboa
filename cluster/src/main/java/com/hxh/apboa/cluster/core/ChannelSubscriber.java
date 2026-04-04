package com.hxh.apboa.cluster.core;

import org.springframework.data.redis.listener.Topic;

/**
 * 描述：Redis频道订阅者接口
 * 实现此接口并注册为Spring Bean，即可自动订阅Redis频道
 *
 * @author huxuehao
 **/
public interface ChannelSubscriber {

    /**
     * 获取订阅的频道主题
     *
     * @return 频道主题（支持PatternTopic或ChannelTopic）
     */
    Topic getTopic();

    /**
     * 处理接收到的消息
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    void onMessage(String channel, String message);
}
