package com.hxh.apboa.studio.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.StudioConfig;

import java.util.List;

/**
 * 描述：StudioConfigService
 *
 * @author huxuehao
 **/
public interface StudioConfigService extends IService<StudioConfig> {
    List<Object> usedWithAgent(List<Long> ids);
}
