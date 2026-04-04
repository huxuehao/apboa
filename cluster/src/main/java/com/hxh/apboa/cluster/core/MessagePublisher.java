package com.hxh.apboa.cluster.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 描述：Redis消息发布工具
 * 用于向Redis频道发布消息
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final StringRedisTemplate redisTemplate;

    /**
     * 向指定频道发布消息
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public void publish(String channel, String message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.debug("发布Redis消息成功 - channel: {}", channel);
        } catch (Exception e) {
            log.error("发布Redis消息失败 - channel: {}, error: {}", channel, e.getMessage(), e);
        }
    }
}
