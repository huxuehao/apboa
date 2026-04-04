package com.hxh.apboa.cluster.config;

import com.hxh.apboa.cluster.core.ChannelSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 描述：Redis集群消息配置
 * 统一管理Redis发布订阅的监听容器，自动注册所有ChannelSubscriber
 *
 * @author huxuehao
 **/
@Slf4j
@Configuration
public class ClusterRedisConfig {

    /**
     * 配置Redis消息监听容器
     * 自动发现并注册所有实现了ChannelSubscriber接口的订阅者
     *
     * @param connectionFactory Redis连接工厂
     * @param subscribers 所有的频道订阅者（Spring自动注入）
     * @return Redis消息监听容器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Autowired(required = false) List<ChannelSubscriber> subscribers) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 注册所有订阅者
        if (subscribers != null && !subscribers.isEmpty()) {
            for (ChannelSubscriber subscriber : subscribers) {
                container.addMessageListener((message, pattern) -> {
                    try {
                        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                        String body = new String(message.getBody(), StandardCharsets.UTF_8);
                        subscriber.onMessage(channel, body);
                    } catch (Exception e) {
                        log.error("处理Redis消息失败 - channel: {}, error: {}",
                                new String(message.getChannel(), StandardCharsets.UTF_8), e.getMessage(), e);
                    }
                }, subscriber.getTopic());
                log.info("注册Redis订阅者: {} -> {}", subscriber.getClass().getSimpleName(), subscriber.getTopic());
            }
        }

        return container;
    }
}
