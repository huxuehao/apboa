package com.hxh.apboa.skill.service;

import com.hxh.apboa.common.entity.SkillPackage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 技能包Service
 *
 * @author huxuehao
 */
public interface SkillPackageService extends IService<SkillPackage> {
    List<Object> usedWithAgent(List<Long> ids);

    /**
     * 获取所有分类
     *
     * @return 分类列表
     */
    List<String> listCategories();

    boolean deleteByIds(List<Long> ids);

    /**
     * 更新技能包并触发关联智能体重新注册
     *
     * @param entity 技能包
     * @return 是否成功
     */
    boolean doUpdate(SkillPackage entity);
}
