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
}
