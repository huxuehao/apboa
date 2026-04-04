package com.hxh.apboa.job.core.job;

import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.job.core.enums.QuartzEnum;
import com.hxh.apboa.job.core.enums.QuartzResult;
import com.hxh.apboa.common.entity.JobLog;
import com.hxh.apboa.job.core.cluster.JobDistributedLock;
import com.hxh.apboa.job.service.QuartzLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 描述：任务执行器
 * 定时任务的实际执行逻辑需要继承该抽象类，并重新doJob方法。
 * 该类提供了操作spring-bean的快捷方法。
 * 该类提供了前置后置执行方法。
 *
 * @author huxuehao
 **/
@Slf4j
@DisallowConcurrentExecution
public abstract class QuartzJob implements Job {
    private JobExecutionContext context;

    /**
     * 锁默认过期时间（秒）
     * 默认15分钟，防止任务执行时间过长导致锁过期
     */
    private static final long LOCK_EXPIRE_SECONDS = 1500;

    @Override
    public void execute(JobExecutionContext context) {
        this.context = context;

        // 任务身份ID
        String identity = getDataMap(QuartzEnum.IDENTITY_KEY.value(), String.class);

        // 随机延迟0-500ms，避免多节点同时抢占
        randomDelay();

        // 获取分布式锁
        JobDistributedLock distributedLock = getBean(JobDistributedLock.class);
        boolean locked = false;

        try {
            // 使用带负载均衡的锁获取
            locked = distributedLock.tryLockWithBalance(identity, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.debug("任务已被其他节点执行或负载均衡放弃，跳过 - jobId: {}", identity);
                return;
            }

            // 执行实际任务
            doExecute(context, identity);

            // 执行成功，记录执行历史
            distributedLock.recordExecHistory(identity);

        } finally {
            if (locked) {
                distributedLock.unlock(identity);
            }
        }
    }

    /**
     * 随机延迟，用于负载均衡
     */
    private void randomDelay() {
        try {
            long delay = ThreadLocalRandom.current().nextLong(0, 500);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 实际执行任务逻辑
     */
    private void doExecute(JobExecutionContext context, String identity) {
        QuartzLogService quartzLogService = getBean(QuartzLogService.class);

        // 开始时间毫秒
        long startTimeMillis = System.currentTimeMillis();
        // 开始时间
        Date startTime = new Date();
        JobLog jobLog = null;
        Object result;
        try {
            // 前置方案
            beforeDoJob(context);
            // 执行日志
            result = doJob(context);
            // 后置方法
            afterDoJob(context, result);
            // 获取执行日志
            Object runMsg = context.getMergedJobDataMap().get(QuartzEnum.RUN_MSG.value());
            // 结束时间毫秒
            long endTimeMillis = System.currentTimeMillis();
            // 结束时间
            Date endTime = new Date();
            // 计算执行时长
            long duration = (endTimeMillis - startTimeMillis) / 1000;
            // 记录日志
            jobLog = new JobLog(identity, startTime, endTime, duration, "任务执行成功!\r\n" + (runMsg != null? runMsg.toString():""), QuartzResult.STATUS_SUCCESS.value());
        } catch (Exception ex) {
            // 结束时间毫秒
            long endTimeMillis = System.currentTimeMillis();
            // 结束时间
            Date endTime = new Date();
            // 计算执行时长
            long duration = (endTimeMillis - startTimeMillis) / 1000;
            // 记录日志
            jobLog = new JobLog(identity, startTime, endTime, duration, "任务执行失败!\r\n" + getExceptionInfo(ex), QuartzResult.STATUS_FAIL.value());
            log.error("执行任务出错:" ,ex);
        } finally {
            if (jobLog != null) {
                quartzLogService.save(jobLog);
            }
        }
    }

    /**
     * 执行前
     * @param context job 上下文
     */
    public void beforeDoJob(JobExecutionContext context) {

    };

    /**
     * 执行
     * @param context job 上下文
     */
    abstract public Object doJob(JobExecutionContext context);

    /**
     * 执行后
     * @param context job 上下文
     * @param result  执行结果
     */
    public void afterDoJob(JobExecutionContext context, Object result) {

    };

    /**
     * 获取DataMap
     */
    protected  <T> T getDataMap(String key, Class<T> clazz) {
        return clazz.cast(context.getMergedJobDataMap().get(key));
    }

    /**
     * 持久化content
     */
    protected  void putRunMsg(Object content) {
        context.getMergedJobDataMap().put(QuartzEnum.RUN_MSG.value(), content);
    }



    /**
     * 获取DataMap
     */
    protected Object getDataMap(String key) {
        return context.getMergedJobDataMap().get(key);
    }

    /**
     * 获取Spring中的Bean
     */
    protected static Object getBean(String name) {
        return BeanUtils.getBean(name);
    }


    /**
     * 获取Spring中的Bean
     */
    protected static <T> T getBean(Class<T> clazz) {
        return BeanUtils.getBean(clazz);
    }
    /**
     * 获取异常信息
     */
    private static String getExceptionInfo(Throwable e) {
        String ret = "";
        if (FuncUtils.isEmpty(e)) {
            return ret;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintStream pout = new PrintStream(out)) {
            e.printStackTrace(pout);
            ret = out.toString();
        } catch (Exception ex) {
            log.error("获取异常信息错误", ex);
        }
        return ret;
    }
}
