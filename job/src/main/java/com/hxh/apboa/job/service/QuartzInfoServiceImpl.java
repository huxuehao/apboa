package com.hxh.apboa.job.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.JobInfo;
import com.hxh.apboa.common.util.CryptoUtils;
import com.hxh.apboa.job.core.client.QuartzClient;
import com.hxh.apboa.job.init.JobInit;
import com.hxh.apboa.job.mapper.JobInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 描述：定时任务管理服务实现
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class QuartzInfoServiceImpl extends ServiceImpl<JobInfoMapper, JobInfo> implements QuartzInfoService {

    private final QuartzClient quartzClient;
    private final AgentDefinitionService agentDefinitionService;

    @Override
    public void updateStatus(JobInfo jobStatus) {
        lambdaUpdate()
                .eq(JobInfo::getId, jobStatus.getId())
                .set(JobInfo::isEnabled, jobStatus.isEnabled())
                .update();
    }

    @Override
    public void addJob(JobInfo jobInfo) {
        checkAgent(jobInfo.getBizId());
        jobInfo.setId(CryptoUtils.uuid());
        try {
            save(jobInfo);
            if (jobInfo.isEnabled()) {
                quartzClient.create(JobInit.buildConfig(jobInfo));
            }
        } catch (Exception e) {
            removeById(jobInfo.getId());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateJob(JobInfo jobInfo) throws ClassNotFoundException {
        JobInfo job = getById(jobInfo.getId());
        if (job == null) {
            throw new RuntimeException("任务实例不存在");
        }
        checkAgent(job.getBizId());
        if (jobInfo.isEnabled()) {
            quartzClient.remove(JobInit.buildConfig(jobInfo));
            quartzClient.create(JobInit.buildConfig(jobInfo));
        } else {
            quartzClient.remove(JobInit.buildConfig(jobInfo));
        }
        updateById(jobInfo);
    }

    @Override
    public void updateJobCron(String id, String cron) throws ClassNotFoundException {
        JobInfo job = getById(id);
        if (job == null) {
            throw new RuntimeException("任务实例不存在");
        }
        checkAgent(job.getBizId());
        if (cron.equals(job.getCron())) {
            return;
        }
        job.setCron(cron);
        if (job.isEnabled()) {
            quartzClient.remove(JobInit.buildConfig(job));
            quartzClient.create(JobInit.buildConfig(job));
        }
        updateById(job);
    }

    @Override
    public boolean deleteJob(String id) throws ClassNotFoundException {
        JobInfo jobInfo = getById(id);
        if (jobInfo == null) {
            return true;
        }
        if (jobInfo.isEnabled()) {
            quartzClient.remove(JobInit.buildConfig(jobInfo));
        }
        return removeById(id);
    }

    @Override
    public void startJob(String id) throws ClassNotFoundException {
        JobInfo jobInfo = getById(id);
        if (jobInfo == null) {
            throw new RuntimeException("任务实例不存在");
        }
        checkAgent(jobInfo.getBizId());
        quartzClient.remove(JobInit.buildConfig(jobInfo));
        quartzClient.create(JobInit.buildConfig(jobInfo));
    }

    @Override
    public void stopJob(String id) throws ClassNotFoundException {
        JobInfo jobInfo = getById(id);
        if (jobInfo == null) {
            throw new RuntimeException("任务实例不存在");
        }
        if (jobInfo.isEnabled()) {
            return;
        }
        quartzClient.remove(JobInit.buildConfig(jobInfo));
    }

    private void checkAgent(String agentId) {
        if (agentId == null) {
            return;
        }
        AgentDefinition agentDefinition = agentDefinitionService.getById(agentId);
        if (agentDefinition != null && !agentDefinition.getEnabled()) {
            throw new RuntimeException("智能体无效，不可设置定时");
        }
    }
}
