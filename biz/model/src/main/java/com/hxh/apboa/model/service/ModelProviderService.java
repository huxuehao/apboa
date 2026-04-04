package com.hxh.apboa.model.service;

import com.hxh.apboa.common.entity.ModelProvider;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 模型提供商Service
 *
 * @author huxuehao
 */
public interface ModelProviderService extends IService<ModelProvider> {
    List<Object> usedWithModel(List<Long> ids);
}
