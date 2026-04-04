package com.hxh.apboa.job.core.cluster.message;

import com.hxh.apboa.common.entity.JobInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 描述：任务控制消息
 * 用于集群节点间同步任务操作
 *
 * @author huxuehao
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobControlMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型枚举
     */
    public enum Action {
        /**
         * 添加任务
         */
        ADD,
        /**
         * 更新任务
         */
        UPDATE,
        /**
         * 删除任务
         */
        DELETE,
        /**
         * 更新Cron表达式
         */
        UPDATE_CRON,
        /**
         * 启动任务
         */
        START,
        /**
         * 停止任务
         */
        STOP
    }

    /**
     * 源节点ID
     */
    private String sourceNodeId;

    /**
     * 操作类型
     */
    private Action action;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 任务信息（ADD/UPDATE时使用）
     */
    private JobInfo jobInfo;

    /**
     * Cron表达式（UPDATE_CRON时使用）
     */
    private String cron;

    /**
     * 消息时间戳
     */
    private Long timestamp;

    /**
     * 创建添加任务消息
     */
    public static JobControlMessage createAddMessage(String sourceNodeId, JobInfo jobInfo) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.ADD)
                .jobId(jobInfo.getId())
                .jobInfo(jobInfo)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建更新任务消息
     */
    public static JobControlMessage createUpdateMessage(String sourceNodeId, JobInfo jobInfo) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.UPDATE)
                .jobId(jobInfo.getId())
                .jobInfo(jobInfo)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建删除任务消息
     */
    public static JobControlMessage createDeleteMessage(String sourceNodeId, String jobId) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.DELETE)
                .jobId(jobId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建更新Cron消息
     */
    public static JobControlMessage createUpdateCronMessage(String sourceNodeId, String jobId, String cron) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.UPDATE_CRON)
                .jobId(jobId)
                .cron(cron)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建启动任务消息
     */
    public static JobControlMessage createStartMessage(String sourceNodeId, String jobId) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.START)
                .jobId(jobId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建停止任务消息
     */
    public static JobControlMessage createStopMessage(String sourceNodeId, String jobId) {
        return JobControlMessage.builder()
                .sourceNodeId(sourceNodeId)
                .action(Action.STOP)
                .jobId(jobId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
