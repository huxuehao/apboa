package com.hxh.apboa.job.core.config;

import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.job.core.client.QuartzScript;
import com.hxh.apboa.job.core.job.QuartzJob;
import lombok.Getter;
import lombok.Setter;
import org.quartz.Job;
import org.quartz.JobDataMap;

/**
 * 描述：quartz 配置
 * 该类是定时任务的核心配置了，定时任务行为（增删改查）的关键
 *
 * @author huxuehao
 **/
public class QuartzConfig {
    private String identity;

    /**
     * cron定时策略
     */
    @Getter
    private String cron;

    /**
     * 任务执行参数-可以传入任意类型的数据给执行类
     */
    @Getter
    private final JobDataMap dataMap = new JobDataMap();

    /**
     * 任务执行类-实现了{@code org.quartz.Job}接口的类
     */
    @Setter
    private Class<? extends QuartzJob> jobClass;

    public String getIdentity() {
        checkIdentity();
        return identity;
    }

    public void setIdentity(String identity) {
        if (FuncUtils.isEmpty(identity)) {
            throw new RuntimeException("identity 不可为空");
        }
        this.identity = identity;
    }

    public void checkIdentity() {
        if (FuncUtils.isEmpty(identity)) {
            throw new RuntimeException("QuartzConfig 中的 identity 未设置");
        }
    }

    public void setCron(String cron) {
        if (QuartzScript.isValidCron(cron)) {
            this.cron = cron;
        } else {
            throw new RuntimeException("cron 不合法");
        }

    }

    public void putDataMap(String key, Object val) {
        this.dataMap.put(key, val);
    }

    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    public void checkJobClass() {
        if (jobClass == null) {
            throw new RuntimeException("QuartzConfig 中的 jobClass 未设置");
        }
    }

    public String getJobName() {
        checkIdentity();
        checkJobClass();
        return "QUARTZ_JOB@" + getIdentity();
    }
    public String getJobGroupName() {
        checkIdentity();
        checkJobClass();
        return "QUARTZ_JOB_GROUP@" + getIdentity();
    }

    public String getTriggerName() {
        checkIdentity();
        checkJobClass();
        return "QUARTZ_TRIGGER@" + getIdentity();
    }

    public String getTriggerGroupName() {
        checkIdentity();
        checkJobClass();
        return "QUARTZ_TRIGGER_GROUP@" + getIdentity();
    }
}
