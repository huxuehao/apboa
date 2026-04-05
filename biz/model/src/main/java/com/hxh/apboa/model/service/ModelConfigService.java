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

    /**
     * 删除模型配置并触发关联智能体重新注册
     *
     * @param ids 模型配置ID列表
     * @return 是否成功
     */
    boolean deleteByIds(List<Long> ids);

    /**
     * 更新模型配置并触发关联智能体重新注册
     *
     * @param entity 模型配置
     * @return 是否成功
     */
    boolean doUpdate(ModelConfig entity);
}
