package com.hxh.apboa.job.consts;

/**
 * 描述：Redis消息通道常量
 * 用于集群节点间通信的频道定义
 *
 * @author huxuehao
 **/
public class JobRedisChannel {

    /**
     * 任务控制消息通道
     * 用于广播任务的增删改启停操作
     */
    public static final String JOB_CLUSTER_CONTROL = "apboa:job:cluster:control";

    /**
     * 节点心跳通道
     * 用于节点状态同步
     */
    public static final String JOB_CLUSTER_HEARTBEAT = "apboa:job:cluster:heartbeat";

    /**
     * 任务执行锁前缀
     */
    public static final String JOB_LOCK_PREFIX = "apboa:job:lock:";

    /**
     * 任务执行历史前缀
     */
    public static final String JOB_EXEC_HISTORY_PREFIX = "apboa:job:exec:history:";

    /**
     * 获取任务执行锁的key
     *
     * @param jobId 任务ID
     * @return 锁key
     */
    public static String getJobLockKey(String jobId) {
        return JOB_LOCK_PREFIX + jobId;
    }

    /**
     * 获取任务执行历史的key
     *
     * @param jobId 任务ID
     * @return 历史key
     */
    public static String getJobExecHistoryKey(String jobId) {
        return JOB_EXEC_HISTORY_PREFIX + jobId;
    }
}
