package com.hxh.apboa.job.core.aspect;

import com.hxh.apboa.common.entity.JobInfo;
import com.hxh.apboa.job.service.QuartzInfoService;

/**
 * 描述：切面抽象类
 *
 * @author huxuehao
 **/
public abstract class QuartzAspect {
    private final QuartzInfoService quartzStatusService;
    public QuartzAspect(QuartzInfoService quartzStatusService) {
        this.quartzStatusService = quartzStatusService;
    }

    public void saveStatus(JobInfo jobStatus) {
        quartzStatusService.updateStatus(jobStatus);
    }
}
