package com.hxh.apboa.job.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.JobInfo;

/**
 * 描述：定时任务管理服务接口
 *
 * @author huxuehao
 **/
public interface QuartzInfoService extends IService<JobInfo> {

    /**
     * 更新任务状态
     *
     * @param jobStatus 包含ID和status的任务对象
     */
    void updateStatus(JobInfo jobStatus);

    /**
     * 新增定时任务
     *
     * @param jobInfo 任务信息
     */
    void addJob(JobInfo jobInfo) throws ClassNotFoundException;

    /**
     * 更新定时任务
     *
     * @param jobInfo 任务信息
     */
    void updateJob(JobInfo jobInfo) throws ClassNotFoundException;

    /**
     * 更新定时任务Cron表达式
     *
     * @param id   任务ID
     * @param cron 新的Cron表达式
     */
    void updateJobCron(String id, String cron) throws ClassNotFoundException;

    /**
     * 删除定时任务
     *
     * @param id 任务ID
     * @return 是否删除成功
     */
    boolean deleteJob(String id) throws ClassNotFoundException;

    /**
     * 启动定时任务
     *
     * @param id 任务ID
     */
    void startJob(String id) throws ClassNotFoundException;

    /**
     * 停止定时任务
     *
     * @param id 任务ID
     */
    void stopJob(String id) throws ClassNotFoundException;
}
