package com.hxh.apboa.hook.service;

import com.hxh.apboa.common.entity.HookConfig;
import com.hxh.apboa.common.wrapper.HookConfigWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Hook配置Service
 *
 * @author huxuehao
 */
public interface HookConfigService extends IService<HookConfig> {
    void SyncConfigToDatabase(List<HookConfigWrapper> configWrappers);
    List<Object> usedWithAgent(List<Long> ids);
    boolean deleteByIds(List<Long> ids);

    /**
     * 更新Hook配置并触发关联智能体重新注册
     *
     * @param entity Hook配置
     * @return 是否成功
     */
    boolean doUpdate(HookConfig entity);
}
