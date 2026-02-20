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
}
