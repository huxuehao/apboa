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
}
