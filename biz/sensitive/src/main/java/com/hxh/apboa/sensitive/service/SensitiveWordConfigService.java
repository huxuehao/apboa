package com.hxh.apboa.sensitive.service;

import com.hxh.apboa.common.entity.SensitiveWordConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 敏感词配置Service
 *
 * @author huxuehao
 */
public interface SensitiveWordConfigService extends IService<SensitiveWordConfig> {
    List<Object> usedWithAgent(List<Long> ids);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<String> listCategories();

    /**
     * 删除敏感词配置并触发关联智能体重新注册
     *
     * @param ids 敏感词配置ID列表
     * @return 是否成功
     */
    boolean deleteByIds(List<Long> ids);

    /**
     * 更新敏感词配置并触发关联智能体重新注册
     *
     * @param entity 敏感词配置
     * @return 是否成功
     */
    boolean doUpdate(SensitiveWordConfig entity);
}
