package com.hxh.apboa.job.core.cluster;

import com.hxh.apboa.job.consts.JobRedisChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 描述：Redis消息监听器配置
 * 配置任务集群控制消息的订阅
 *
 * @author huxuehao
 **/
@Configuration
@RequiredArgsConstructor
public class JobMessageListenerConfig {

    private final JobMessageSubscriber jobMessageSubscriber;

    /**
     * 配置Redis消息监听容器
     *
     * @param connectionFactory Redis连接工厂
     * @return 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer jobMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅任务控制消息频道
        container.addMessageListener(jobMessageSubscriber, new ChannelTopic(JobRedisChannel.JOB_CLUSTER_CONTROL));

        return container;
    }
}
