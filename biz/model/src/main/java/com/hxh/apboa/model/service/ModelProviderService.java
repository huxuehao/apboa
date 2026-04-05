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

    /**
     * 删除模型供应商并触发关联智能体重新注册
     *
     * @param ids 供应商ID列表
     * @return 是否成功
     */
    boolean deleteByIds(List<Long> ids);

    /**
     * 更新模型供应商并触发关联智能体重新注册
     *
     * @param entity 模型供应商
     * @return 是否成功
     */
    boolean doUpdate(ModelProvider entity);
}
