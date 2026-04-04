package com.hxh.apboa.job.core.cluster;

import com.hxh.apboa.job.consts.JobRedisChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 描述：任务分布式锁
 * 基于Redis实现的分布式锁，用于任务抢占执行
 * 支持基于执行历史的负载均衡
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class JobDistributedLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final NodeConfig nodeConfig;

    /**
     * 存储当前线程获取的锁值
     */
    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    /**
     * 执行历史保留数量
     */
    private static final int EXEC_HISTORY_SIZE = 10;

    /**
     * 执行历史过期时间（小时）
     */
    private static final long EXEC_HISTORY_EXPIRE_HOURS = 24;

    /**
     * 负载均衡阈值（当前节点执行次数超过此比例则放弃）
     */
    private static final double BALANCE_THRESHOLD = 0.5;

    /**
     * 最小历史记录数（低于此值不启用负载均衡）
     */
    private static final int MIN_HISTORY_SIZE = 3;

    /**
     * 释放锁的Lua脚本（保证原子性）
     */
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 尝试获取分布式锁（带过期时间）
     *
     * @param jobId      任务ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String jobId, long expireTime, TimeUnit timeUnit) {
        try {
            String lockKey = JobRedisChannel.getJobLockKey(jobId);
            String lockValue = getLockValue();

            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime, timeUnit);

            if (Boolean.TRUE.equals(success)) {
                lockValueHolder.set(lockValue);
                log.debug("获取分布式锁成功 - jobId: {}, node: {}", jobId, nodeConfig.getNodeId());
                return true;
            } else {
                String currentLock = stringRedisTemplate.opsForValue().get(lockKey);
                log.debug("获取分布式锁失败 - jobId: {}, 当前持有者: {}", jobId, currentLock);
                return false;
            }
        } catch (Exception e) {
            log.error("获取分布式锁异常 - jobId: {}", jobId, e);
            return false;
        }
    }

    /**
     * 尝试获取分布式锁（带负载均衡）
     * 根据执行历史决定是否参与竞争
     *
     * @param jobId      任务ID
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return 是否获取成功
     */
    public boolean tryLockWithBalance(String jobId, long expireTime, TimeUnit timeUnit) {
        // 检查是否应该放弃竞争（负载均衡）
        if (shouldSkipForBalance(jobId)) {
            log.debug("负载均衡 - 当前节点执行次数过多，放弃竞争 - jobId: {}, node: {}",
                    jobId, nodeConfig.getNodeId());
            return false;
        }

        return tryLock(jobId, expireTime, timeUnit);
    }

    /**
     * 释放分布式锁
     *
     * @param jobId 任务ID
     * @return 是否释放成功
     */
    public boolean unlock(String jobId) {
        try {
            String lockKey = JobRedisChannel.getJobLockKey(jobId);
            String lockValue = lockValueHolder.get();

            if (lockValue == null) {
                log.warn("释放分布式锁失败 - 未找到锁值，可能未获取锁或已被释放 - jobId: {}", jobId);
                return false;
            }

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(RELEASE_LOCK_SCRIPT);
            redisScript.setResultType(Long.class);

            Long result = stringRedisTemplate.execute(redisScript,
                    Collections.singletonList(lockKey), lockValue);

            boolean success = result != null && result > 0;
            if (success) {
                log.debug("释放分布式锁成功 - jobId: {}, node: {}", jobId, nodeConfig.getNodeId());
            } else {
                log.warn("释放分布式锁失败，可能已被其他节点获取 - jobId: {}", jobId);
            }

            // 无论成功与否，都清理ThreadLocal
            lockValueHolder.remove();
            return success;
        } catch (Exception e) {
            log.error("释放分布式锁异常 - jobId: {}", jobId, e);
            lockValueHolder.remove();
            return false;
        }
    }

    /**
     * 检查锁是否被当前节点持有
     *
     * @param jobId 任务ID
     * @return 是否持有锁
     */
    public boolean isLockedByCurrentNode(String jobId) {
        try {
            String lockKey = JobRedisChannel.getJobLockKey(jobId);
            String lockValue = stringRedisTemplate.opsForValue().get(lockKey);
            return getLockValue().equals(lockValue);
        } catch (Exception e) {
            log.error("检查锁状态异常 - jobId: {}", jobId, e);
            return false;
        }
    }

    /**
     * 记录执行历史
     *
     * @param jobId 任务ID
     */
    public void recordExecHistory(String jobId) {
        try {
            String key = JobRedisChannel.getJobExecHistoryKey(jobId);
            String nodeId = nodeConfig.getNodeId();

            // 追加到List尾部
            stringRedisTemplate.opsForList().rightPush(key, nodeId);

            // 只保留最近N条
            stringRedisTemplate.opsForList().trim(key, -EXEC_HISTORY_SIZE, -1);

            // 设置过期时间（24小时）
            stringRedisTemplate.expire(key, EXEC_HISTORY_EXPIRE_HOURS, TimeUnit.HOURS);

            log.debug("记录执行历史 - jobId: {}, node: {}", jobId, nodeId);
        } catch (Exception e) {
            log.error("记录执行历史异常 - jobId: {}", jobId, e);
        }
    }

    /**
     * 获取执行历史
     *
     * @param jobId 任务ID
     * @return 执行历史列表
     */
    public List<String> getExecHistory(String jobId) {
        try {
            String key = JobRedisChannel.getJobExecHistoryKey(jobId);
            return stringRedisTemplate.opsForList().range(key, 0, -1);
        } catch (Exception e) {
            log.error("获取执行历史异常 - jobId: {}", jobId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 判断是否应放弃竞争（负载均衡）
     *
     * @param jobId 任务ID
     * @return 是否应放弃
     */
    private boolean shouldSkipForBalance(String jobId) {
        try {
            List<String> history = getExecHistory(jobId);

            // 历史记录不足，不启用负载均衡
            if (history.size() < MIN_HISTORY_SIZE) {
                return false;
            }

            // 统计历史中的唯一节点数
            long uniqueNodes = history.stream().distinct().count();
            // 如果历史记录中只有当前节点，说明可能是单节点或新任务，不启用负载均衡
            if (uniqueNodes <= 1) {
                return false;
            }

            // 统计当前节点执行次数
            String currentNodeId = nodeConfig.getNodeId();
            long currentNodeCount = history.stream()
                    .filter(nodeId -> nodeId.equals(currentNodeId))
                    .count();

            // 计算占比
            double ratio = (double) currentNodeCount / history.size();

            // 超过阈值则放弃
            boolean shouldSkip = ratio > BALANCE_THRESHOLD;

            if (shouldSkip) {
                log.info("负载均衡触发 - jobId: {}, 当前节点执行占比: {}/{} = {:.0%}",
                        jobId, currentNodeCount, history.size(), ratio);
            }

            return shouldSkip;
        } catch (Exception e) {
            log.error("负载均衡判断异常 - jobId: {}", jobId, e);
            return false;
        }
    }

    /**
     * 获取锁的值（节点标识）
     *
     * @return 锁值
     */
    private String getLockValue() {
        return nodeConfig.getNodeId() + ":" + System.currentTimeMillis();
    }
}
