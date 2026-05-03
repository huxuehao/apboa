package com.hxh.apboa.core;

import com.hxh.apboa.common.util.AgentMetadataStore;
import io.agentscope.spring.boot.agui.common.ThreadSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 描述：定期清除代理元数据，避免内存溢出
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class ClearAgentMetadataStore {
    private final ThreadSessionManager sessionManager;

    /**
     * 每天凌晨3点执行，清理无关联会话的 Agent 元数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void executeAt3AM() {
        log.info("开始清理过期 Agent 元数据，当前存储数量: {}", AgentMetadataStore.STORE.size());
        AtomicInteger removedCount = new AtomicInteger();

        // 先收集待删除的 key，避免并发修改异常
        List<String> toRemove = new ArrayList<>();

        AgentMetadataStore.STORE.forEach((agentId, meta) -> {
            Object threadIdObj = meta.get("threadId");
            if (threadIdObj == null) {
                // threadId 不存在，标记删除
                toRemove.add(agentId);
                return;
            }

            String threadId = threadIdObj.toString();
            try {
                if (sessionManager.getSession(threadId).isEmpty()) {
                    toRemove.add(agentId);
                }
            } catch (Exception e) {
                log.warn("检查会话 {} 时出错，将清理对应元数据", threadId, e);
                toRemove.add(agentId);
            }
        });

        // 批量删除
        toRemove.forEach(agentId -> {
            AgentMetadataStore.remove(agentId);
            removedCount.getAndIncrement();
        });

        log.info("清理完成，移除 {} 条记录，剩余: {}", removedCount, AgentMetadataStore.STORE.size());
    }
}
