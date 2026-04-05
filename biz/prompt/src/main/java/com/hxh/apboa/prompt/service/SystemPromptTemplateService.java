package com.hxh.apboa.prompt.service;

import com.hxh.apboa.common.entity.SystemPromptTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统提示词模板Service
 *
 * @author huxuehao
 */
public interface SystemPromptTemplateService extends IService<SystemPromptTemplate> {
    List<Object> usedWithAgent(List<Long> ids);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<String> listCategories();

    /**
     * 删除提示词模板并触发关联智能体重新注册
     *
     * @param ids 模板ID列表
     * @return 是否成功
     */
    boolean deleteByIds(List<Long> ids);

    /**
     * 更新提示词模板并触发关联智能体重新注册
     *
     * @param entity 提示词模板
     * @return 是否成功
     */
    boolean doUpdate(SystemPromptTemplate entity);
}
