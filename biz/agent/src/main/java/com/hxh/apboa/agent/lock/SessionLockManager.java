package com.hxh.apboa.agent.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 描述：基于 sessionId 的细粒度锁管理器
 * 为每个 sessionId 提供独立的 ReentrantLock，不同 session 之间互不阻塞，
 * 同一 session 内的消息追加操作串行化执行，避免并发导致消息 path 冲突。
 * 自带安全自动清理，无内存泄漏
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class SessionLockManager {

    /**
     * sessionId -> ReentrantLock 映射，使用 ConcurrentHashMap 保证线程安全
     */
    private final ConcurrentHashMap<Long, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 获取指定 sessionId 对应的锁（不存在则创建）
     */
    public ReentrantLock getLock(Long sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId 不能为 null");
        }
        return lockMap.computeIfAbsent(sessionId, k -> new ReentrantLock());
    }

    /**
     * 尝试清理不再使用的锁，避免 Map 无限增长
     * 仅在：无等待线程 + 能成功 trylock 时才移除
     * 绝对安全，不会影响正在使用的锁
     */
    public void cleanupIfUnused(Long sessionId, ReentrantLock lock) {
        if (sessionId == null || lock == null) {
            return;
        }

        // 没有其他线程在等待这个锁，才允许尝试清理
        if (!lock.hasQueuedThreads()) {
            // 尝试立即获取锁（能获取到说明当前已完全释放）
            if (lock.tryLock()) {
                try {
                    // 原子移除：只有锁还是原来的对象，才删除
                    boolean removed = lockMap.remove(sessionId, lock);
                    if (removed && log.isDebugEnabled()) {
                        log.debug("[会话锁清理] 成功清理 sessionId={}，当前锁数量={}",
                                sessionId, lockMap.size());
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
