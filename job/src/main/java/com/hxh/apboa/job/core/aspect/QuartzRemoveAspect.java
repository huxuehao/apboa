package com.hxh.apboa.job.core.aspect;

import com.hxh.apboa.job.core.config.QuartzConfig;
import com.hxh.apboa.job.core.enums.QuartzStatus;
import com.hxh.apboa.common.entity.JobInfo;
import com.hxh.apboa.job.service.QuartzInfoService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 描述：删除调度任务切面
 *
 * @author huxuehao
 **/
@Aspect
@Order(-1) /* 该切面应当先于 @Transactional 执行 */
@Component
public class QuartzRemoveAspect extends QuartzAspect {
    public QuartzRemoveAspect(QuartzInfoService quartzStatusService) {
        super(quartzStatusService);
    }

    @Pointcut("@annotation(com.hxh.apboa.job.core.annotation.QuartzRemove)")
    public void quartzRemovePointcut() {

    }

    @AfterReturning(pointcut = "quartzRemovePointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        if (result == null) return;
        QuartzConfig config = (QuartzConfig)result;
        JobInfo jobInfo = new JobInfo();
        jobInfo.setId(config.getIdentity());
        jobInfo.setCron(config.getCron());
        jobInfo.setEnabled(QuartzStatus.REMOVE.value());
        saveStatus(jobInfo);
    }
}
