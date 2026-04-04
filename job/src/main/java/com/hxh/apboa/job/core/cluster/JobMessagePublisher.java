package com.hxh.apboa.job.core.cluster;

import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.job.consts.JobRedisChannel;
import com.hxh.apboa.job.core.cluster.message.JobControlMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 描述：任务消息发布者
 * 负责向Redis发布集群控制消息
 *
 * @author huxuehao
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class JobMessagePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final NodeConfig nodeConfig;

    /**
     * 发布任务控制消息
     *
     * @param message 控制消息
     */
    public void publish(JobControlMessage message) {
        try {
            String jsonMessage = JsonUtils.toJsonStr(message);
            stringRedisTemplate.convertAndSend(JobRedisChannel.JOB_CLUSTER_CONTROL, jsonMessage);
            log.debug("发布任务控制消息成功 - action: {}, jobId: {}, sourceNode: {}",
                    message.getAction(), message.getJobId(), message.getSourceNodeId());
        } catch (Exception e) {
            log.error("发布任务控制消息失败 - action: {}, jobId: {}",
                    message.getAction(), message.getJobId(), e);
        }
    }

    /**
     * 发布添加任务消息
     *
     * @param jobInfo 任务信息
     */
    public void publishAdd(com.hxh.apboa.common.entity.JobInfo jobInfo) {
        JobControlMessage message = JobControlMessage.createAddMessage(nodeConfig.getNodeId(), jobInfo);
        publish(message);
    }

    /**
     * 发布更新任务消息
     *
     * @param jobInfo 任务信息
     */
    public void publishUpdate(com.hxh.apboa.common.entity.JobInfo jobInfo) {
        JobControlMessage message = JobControlMessage.createUpdateMessage(nodeConfig.getNodeId(), jobInfo);
        publish(message);
    }

    /**
     * 发布删除任务消息
     *
     * @param jobId 任务ID
     */
    public void publishDelete(String jobId) {
        JobControlMessage message = JobControlMessage.createDeleteMessage(nodeConfig.getNodeId(), jobId);
        publish(message);
    }

    /**
     * 发布更新Cron消息
     *
     * @param jobId 任务ID
     * @param cron  Cron表达式
     */
    public void publishUpdateCron(String jobId, String cron) {
        JobControlMessage message = JobControlMessage.createUpdateCronMessage(nodeConfig.getNodeId(), jobId, cron);
        publish(message);
    }

    /**
     * 发布启动任务消息
     *
     * @param jobId 任务ID
     */
    public void publishStart(String jobId) {
        JobControlMessage message = JobControlMessage.createStartMessage(nodeConfig.getNodeId(), jobId);
        publish(message);
    }

    /**
     * 发布停止任务消息
     *
     * @param jobId 任务ID
     */
    public void publishStop(String jobId) {
        JobControlMessage message = JobControlMessage.createStopMessage(nodeConfig.getNodeId(), jobId);
        publish(message);
    }
}
