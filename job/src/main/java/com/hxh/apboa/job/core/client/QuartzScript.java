package com.hxh.apboa.job.core.client;

import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.job.core.config.QuartzConfig;
import com.hxh.apboa.job.core.enable.QuartzEnabled;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

/**
 * 描述：quartz 脚本
 * 这个是整个定时任务的核心，是对quartz的二次封装，包括了定时任务的多种行为操作
 * QuartzConfig是该脚本类的首席参数
 *
 * @author huxuehao
 **/
@Slf4j
public class QuartzScript {
    private static boolean ENABLE = false;
    private static final SchedulerFactory SCHEDULER_FACTORY = new StdSchedulerFactory();

    /**
     * 启动调度任务
     *
     * @param jobDetail 执行任务
     * @param trigger   触发器
     */
    private static void startScheduler(JobDetail jobDetail, Trigger trigger) {
        try {
            // 获取调度任务实例
            Scheduler scheduler = scheduler();
            // 绑定触发器和调度任务
            scheduler.scheduleJob(jobDetail, trigger);
            // 启动
            scheduler.start();
        } catch (SchedulerException e) {
            String msg = "启动调度任务失败";
            log.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    /**
     * 任务执行实例
     *
     * @param jobName      任务名称
     * @param jobGroupName 任务分组名
     * @param jobDataMap   执行参数
     * @param jobClass     执行类-实现了{@code org.quartz.Job}接口的类
     * @return {@link JobDetail}
     */
    private static JobDetail getJobDetail(String jobName, String jobGroupName, JobDataMap jobDataMap, Class<? extends Job> jobClass) {
        // 获取任务执行类的实例
        JobBuilder jobBuilder = JobBuilder.newJob(jobClass);
        // 添加任务名，任务组
        jobBuilder.withIdentity(jobName, jobGroupName);
        // 添加任务执行的参数
        if (jobDataMap != null && !jobDataMap.isEmpty()) {
            jobBuilder.setJobData(jobDataMap);
        }
        // 获取job执行实例
        return jobBuilder.build();
    }

    /**
     * 一次任务触发器
     *
     * @param triggerName      触发器名
     * @param triggerGroupName 触发器分组名
     * @return {@link SimpleTrigger}
     */
    private static SimpleTrigger getOnceTrigger(String triggerName, String triggerGroupName) {
        // 触发器
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        // 触发器名,触发器组
        triggerBuilder.withIdentity(triggerName, triggerGroupName);
        // 3秒后立即执行，重复次数设为0，表示只执行一次
        triggerBuilder.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(3).withRepeatCount(0));
        // 从当前时间判断触发
        triggerBuilder.startNow();
        // 创建Trigger对象
        return (SimpleTrigger) triggerBuilder.build();
    }

    /**
     * 判断是否启用调度器
     */
    private static boolean isQuartzEnabled() {
        if (!ENABLE) {
            try {
                BeanUtils.getBean(QuartzEnabled.class);
                ENABLE = true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取调度器
     */
    protected static Scheduler scheduler() throws SchedulerException {
        if (!isQuartzEnabled()) {
            throw new RuntimeException("quartz 不可用，quartzd.enabled 必须为 true");
        }
        return SCHEDULER_FACTORY.getScheduler();
    }

    /**
     * 创建添加定时任务
     *
     * @param config 任务参数
     */
    protected static void createCronJob(QuartzConfig config) {
        if (!isQuartzEnabled()) {
            throw new RuntimeException("quartz 不可用，quartzd.enabled 必须为 true");
        }
        // 判断定时任务是否存在
        String triggerState = getTriggerState(config);
        if (Trigger.TriggerState.NONE.name().equals(triggerState)) {
            JobDetail jobDetail = getJobDetail(config.getJobName(), config.getJobGroupName(), config.getDataMap(), config.getJobClass());
            CronTrigger cronTrigger = getCronTrigger(config.getTriggerName(), config.getTriggerGroupName(), config.getCron());
            // 启动调度任务
            startScheduler(jobDetail, cronTrigger);
        } else {
            log.warn("job 已存在");
        }
    }

    /**
     * 创建添加只执行一次的任务
     *
     * @param config  任务参数
     * @param force   是否强制执行
     */
    protected static void createOnceJob(QuartzConfig config, boolean force) {
        if (!isQuartzEnabled()) {
            throw new RuntimeException("quartz 不可用，quartzd.enabled 必须为 true");
        }
        // 判断定时任务是否存在
        String triggerState = getTriggerState(config);
        if (Trigger.TriggerState.NONE.name().equals(triggerState) || force) {
            String jobName = config.getJobName();
            String triggerName = config.getTriggerName();
            if (force) {
                jobName = jobName + "executeOnce";
                triggerName = triggerName + "executeOnce";
            }
            JobDetail jobDetail = getJobDetail(jobName, config.getJobGroupName(), config.getDataMap(), config.getJobClass());
            SimpleTrigger simpleTrigger = getOnceTrigger(triggerName, config.getTriggerGroupName());
            // 启动调度任务
            startScheduler(jobDetail, simpleTrigger);
        }
    }

    /**
     * 修改定时任务
     *
     * @param triggerName      触发器名称
     * @param triggerGroupName 触发器组名称
     * @param cron             cron表达式
     */
    protected static void updateCron(String triggerName, String triggerGroupName, String cron) {
        try {
            // 获取调度任务实例
            Scheduler scheduler = QuartzScript.scheduler();;
            // 获取原来触发器
            TriggerKey triggerKey = TriggerKey.triggerKey(triggerName, triggerGroupName);
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger != null) {
                String oldTime = trigger.getCronExpression();
                if (!oldTime.equalsIgnoreCase(cron)) {
                    CronTrigger cronTrigger = QuartzScript.getCronTrigger(triggerName, triggerGroupName, cron);
                    // 修改一个任务的触发时间
                    scheduler.rescheduleJob(triggerKey, cronTrigger);
                }
            }
        } catch (SchedulerException e) {
            String msg = "修改调度任务触发时间失败";
            log.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    /**
     * 移除定时任务
     *
     * @param config 任务参数
     */
    protected static void removeJob(QuartzConfig config) {
        try {
            Scheduler scheduler = scheduler();
            TriggerKey triggerKey = TriggerKey.triggerKey(config.getTriggerName(), config.getTriggerGroupName());
            // 停止触发器
            scheduler.pauseTrigger(triggerKey);
            // 移除触发器
            scheduler.unscheduleJob(triggerKey);

            JobKey jobKey = JobKey.jobKey(config.getJobName(), config.getJobGroupName());
            // 停止任务
            scheduler.interrupt(jobKey);
            // 删除任务
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            String msg = "停止定时任务失败";
            log.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    /**
     * 获取触发器状态
     *
     * @param config 任务参数
     * @return
     * NONE:    不存在
     * NORMAL:  正常
     * PAUSED:  暂停
     * COMPLETE:完成
     * ERROR :  错误
     * BLOCKED :阻塞
     */
    protected static String getTriggerState(QuartzConfig config) {
        if (!isQuartzEnabled()) {
            throw new RuntimeException("quartz 不可用，quartzd.enabled 必须为 true");
        }
        TriggerKey triggerKey = TriggerKey.triggerKey(config.getTriggerName(), config.getTriggerGroupName());
        try {
            Scheduler scheduler = scheduler();
            Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);
            return triggerState.name();
        } catch (SchedulerException e) {
            String msg = "获取触发器状态失败";
            log.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    /**
     * cron触发器
     *
     * @param triggerName      触发器名
     * @param triggerGroupName 触发器分组名
     * @param cron             定时策略
     * @return {@link CronTrigger}
     */
    protected static CronTrigger getCronTrigger(String triggerName, String triggerGroupName, String cron) {
        if (!isQuartzEnabled()) {
            throw new RuntimeException("quartz 不可用，quartzd.enabled 必须为 true");
        }
        // 触发器
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger();
        // 触发器名,触发器组
        triggerBuilder.withIdentity(triggerName, triggerGroupName);
        // 触发器时间设定
        triggerBuilder.withSchedule(CronScheduleBuilder.cronSchedule(cron));
        // 从当前时间判断触发
        triggerBuilder.startNow();
        // 创建Trigger对象
        return (CronTrigger) triggerBuilder.build();
    }

    /**
     * 判断cron表达式是否合法
     * @param cron cron表达式
     */
    public static boolean isValidCron(String cron) {
        return CronExpression.isValidExpression(cron);
    }
}
