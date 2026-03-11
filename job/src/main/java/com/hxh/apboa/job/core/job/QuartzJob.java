package com.hxh.apboa.job.core.job;

import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.FuncUtils;
import com.hxh.apboa.job.core.enums.QuartzEnum;
import com.hxh.apboa.job.core.enums.QuartzResult;
import com.hxh.apboa.common.entity.JobLog;
import com.hxh.apboa.job.service.QuartzLogService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Date;

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

    @Override
    public void execute(JobExecutionContext context) {
        this.context = context;
        QuartzLogService quartzLogService = getBean(QuartzLogService.class);

        // 任务身份ID
        String identity = getDataMap(QuartzEnum.IDENTITY_KEY.value(), String.class);
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
