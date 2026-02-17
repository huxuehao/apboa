package com.hxh.apboa.model.service;

import com.hxh.apboa.common.entity.ModelConfig;
import com.hxh.apboa.common.wrapper.ModelWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 模型配置Service
 *
 * @author huxuehao
 */
public interface ModelConfigService extends IService<ModelConfig> {
    ModelWrapper getModelWrapperById(Long id);
    List<Object> usedWithAgent(List<Long> ids);
}
