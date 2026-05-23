package com.hxh.apboa.skill.service;

import com.hxh.apboa.common.entity.SkillFile;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 描述：技能包文件Service
 *
 * @author huxuehao
 **/
public interface SkillFileService extends IService<SkillFile> {

    /**
     * 根据技能包ID获取所有文件
     *
     * @param skillId 技能包ID
     * @return 文件列表（按 file_type + file_path 排序）
     */
    List<SkillFile> listBySkillId(Long skillId);

    /**
     * 根据技能包ID和路径精确查找文件
     *
     * @param skillId  技能包ID
     * @param filePath 文件相对路径
     * @return 文件实体，不存在返回 null
     */
    SkillFile getBySkillIdAndPath(Long skillId, String filePath);

    /**
     * 更新文件内容
     *
     * @param fileId  文件ID
     * @param content 新内容
     * @return 是否成功
     */
    boolean updateContent(Long fileId, String content);

    /**
     * 删除技能包下所有文件记录
     *
     * @param skillId 技能包ID
     * @return 是否成功
     */
    boolean deleteBySkillId(Long skillId);

    /**
     * 删除技能包下指定路径的文件记录
     *
     * @param skillId  技能包ID
     * @param filePath 文件相对路径
     * @return 是否成功
     */
    boolean removeBySkillIdAndPath(Long skillId, String filePath);

    /**
     * 删除技能包下指定路径前缀的所有文件记录
     *
     * @param skillId    技能包ID
     * @param pathPrefix 路径前缀，如 "references/"
     * @return 是否成功
     */
    boolean removeBySkillIdAndPathPrefix(Long skillId, String pathPrefix);
}
