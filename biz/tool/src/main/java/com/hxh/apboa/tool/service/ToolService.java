package com.hxh.apboa.tool.service;

import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.wrapper.ToolInfoWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 工具Service
 *
 * @author huxuehao
 */
public interface ToolService extends IService<ToolConfig> {
    Boolean deleteTools(List<Long> ids);

    void SyncConfigToDatabase(List<ToolInfoWrapper> toolInfos);

    List<Object> usedWithAgent(List<Long> ids);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<String> listCategories();

    Boolean doUpdate(ToolConfig toolConfig);
}
