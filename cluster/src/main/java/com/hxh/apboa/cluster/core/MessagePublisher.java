package com.hxh.apboa.cluster.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     * 消息发布专用线程池，避免 afterCommit 回调阻塞事务线程
     */
    private ScheduledExecutorService scheduler;

    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {100, 300, 500};

    @PostConstruct
    public void init() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "redis-publish");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * 向指定频道发布消息（立即发送，不经事务控制，无重试）
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public void publish(String channel, String message) {
        publishWithRetry(channel, message);
    }

    /**
     * 事务提交后异步向指定频道发布消息（支持3次重试）
     * 若当前无活跃事务，则立即异步发布；否则注册为 afterCommit 回调，
     * 回调中提交到线程池异步执行，结合重试机制确保消息可靠送达。
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public void publishAfterCommit(String channel, String message) {
        boolean synchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();
        boolean transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        if (synchronizationActive && transactionActive) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduler.execute(() -> publishWithRetry(channel, message));
                }
            });
        } else {
            scheduler.execute(() -> publishWithRetry(channel, message));
        }
    }

    /**
     * 带重试的 Redis 消息发布
     * 首次尝试同步执行，后续重试通过 scheduler.schedule() 异步调度，
     * 延迟期间不占用线程，避免阻塞线程池。
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    private void publishWithRetry(String channel, String message) {
        doPublishWithRetry(channel, message, 0);
    }

    /**
     * 递归式重试发布
     *
     * @param channel 频道名称
     * @param message 消息内容
     * @param attempt 当前尝试次数（0-based）
     */
    private void doPublishWithRetry(String channel, String message, int attempt) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.debug("发布Redis消息成功 - channel: {}", channel);
        } catch (Exception e) {
            if (attempt < MAX_RETRIES - 1) {
                log.warn("发布Redis消息失败，第{}次重试 - channel: {}, error: {}", attempt + 1, channel, e.getMessage());
                scheduler.schedule(
                        () -> doPublishWithRetry(channel, message, attempt + 1),
                        RETRY_DELAYS_MS[attempt],
                        TimeUnit.MILLISECONDS);
            } else {
                log.error("发布Redis消息最终失败（已重试{}次） - channel: {}, error: {}", MAX_RETRIES, channel, e.getMessage(), e);
            }
        }
    }
}
