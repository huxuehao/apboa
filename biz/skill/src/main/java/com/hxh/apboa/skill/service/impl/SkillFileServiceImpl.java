package com.hxh.apboa.skill.service.impl;

import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.skill.mapper.SkillFileMapper;
import com.hxh.apboa.skill.service.SkillFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 描述：技能包文件Service实现
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class SkillFileServiceImpl extends ServiceImpl<SkillFileMapper, SkillFile> implements SkillFileService {

    @Override
    public List<SkillFile> listBySkillId(Long skillId) {
        return lambdaQuery()
                .eq(SkillFile::getSkillId, skillId)
                .orderByAsc(SkillFile::getFileType)
                .orderByAsc(SkillFile::getSort)
                .orderByAsc(SkillFile::getFileName)
                .list();
    }

    @Override
    public SkillFile getBySkillIdAndPath(Long skillId, String filePath) {
        return lambdaQuery()
                .eq(SkillFile::getSkillId, skillId)
                .eq(SkillFile::getFilePath, filePath)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long fileId, String content) {
        SkillFile file = new SkillFile();
        file.setId(fileId);
        file.setContent(content);
        return updateById(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBySkillId(Long skillId) {
        return remove(new LambdaQueryWrapper<SkillFile>()
                .eq(SkillFile::getSkillId, skillId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBySkillIdAndPath(Long skillId, String filePath) {
        return remove(new LambdaQueryWrapper<SkillFile>()
                .eq(SkillFile::getSkillId, skillId)
                .eq(SkillFile::getFilePath, filePath));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBySkillIdAndPathPrefix(Long skillId, String pathPrefix) {
        return remove(new LambdaQueryWrapper<SkillFile>()
                .eq(SkillFile::getSkillId, skillId)
                .likeRight(SkillFile::getFilePath, pathPrefix));
    }
}
