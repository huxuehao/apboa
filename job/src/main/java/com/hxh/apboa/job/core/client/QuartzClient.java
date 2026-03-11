package com.hxh.apboa.job.core.client;

import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.job.core.annotation.QuartzCreate;
import com.hxh.apboa.job.core.annotation.QuartzRemove;
import com.hxh.apboa.job.core.config.QuartzConfig;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;

/**
 * 描述：调度器客户端
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class QuartzClient {
    /**
     * 创建添加定时任务
     *
     * @param config 任务参数
     */
    @QuartzCreate
    public void create(QuartzConfig config) {
        if (FuncUtils.isEmpty(config.getCron())) {
            QuartzScript.createOnceJob(config, false);
        } else {
            QuartzScript.createCronJob(config);
        }
    }

    /**
     * 创建添加定时任务,仅执行一次
     *
     * @param config 任务参数
     * @param force  是否强制执行
     *               true: 无论定时任务是否存在都会执行
     *               false：若定时任务存在，则不会执行
     */
    public void createOnce(QuartzConfig config, boolean force) {
        QuartzScript.createOnceJob(config, force);
        config.getIdentity();
    }

    /**
     * 修改定时任务Cron规则
     *
     * @param config 任务参数
     */
    @QuartzCreate
    public void updateCron(QuartzConfig config) {
        if (QuartzScript.isValidCron(config.getCron())) {
            QuartzScript.updateCron(config.getTriggerName(), config.getTriggerGroupName(), config.getCron());
        } else {
            throw new RuntimeException("Cron ["+config.getCron()+"] 存在问题");
        }
    }

    /**
     * 移除定时任务
     *
     * @param config 任务参数
     */
    @QuartzRemove
    public void remove(QuartzConfig config) {
        // 定时任务不存在就不处理
        String triggerState = QuartzScript.getTriggerState(config);
        if (Trigger.TriggerState.NONE.name().equals(triggerState)) {
            return;
        }
        QuartzScript.removeJob(config);
    }
}
