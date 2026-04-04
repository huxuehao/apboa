package com.hxh.apboa.knowledge.service;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识库配置Service
 *
 * @author huxuehao
 */
public interface KnowledgeBaseConfigService extends IService<KnowledgeBaseConfig> {
    List<Object> usedWithAgent(List<Long> ids);

    KnowledgeBaseConfig getByAgentId(Long agentId);

    boolean deleteByIds(List<Long> ids);
}
