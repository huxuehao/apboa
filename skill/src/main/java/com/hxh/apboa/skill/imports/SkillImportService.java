package com.hxh.apboa.skill.imports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.skill.imports.config.GitImportConfig;
import com.hxh.apboa.skill.imports.config.LocalImportConfig;
import com.hxh.apboa.skill.imports.config.UploadImportConfig;
import com.hxh.apboa.skill.service.SkillPackageService;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.skill.repository.GitSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import cn.hutool.core.io.FileUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述：技能包导入服务
 *
 * @author huxuehao
 **/
@Service
@RequiredArgsConstructor
public class SkillImportService {
    private final SkillPackageService skillPackageService;

    /**
     * 从 Git 导入
     * @param config 配置
     */
    public boolean importFromGit(GitImportConfig config) {
       try (AgentSkillRepository repo = new GitSkillRepository(config.getRepoUrl())) {
           doImport(repo, List.of(), config.isCover(), config.getCategory());
       }
       return true;
    }

    /**
     * 从本地导入
     * @param config 配置
     */
    public boolean importFromLocal(LocalImportConfig config) {
        try (AgentSkillRepository repo = new FileSystemSkillRepository(Path.of(config.getPath()))) {
            doImport(repo, List.of(), config.isCover(), config.getCategory());
        }
        return true;
    }

    /**
     * 从上传压缩包导入，需要删除临时文件
     * 调用该方法前，需要将上传的压缩包解压到临时文件中，完成入库后需要删除临时文件
     * @param config 配置
     */
    public boolean importFromUpload(UploadImportConfig config) {
        // templatePath 指向解压后的 skills 目录，其上一层即为 UUID 临时目录
        Path skillsPath = Path.of(config.getTemplatePath());
        try (AgentSkillRepository repo = new FileSystemSkillRepository(skillsPath)) {
            doImport(repo, List.of(), config.isCover(), config.getCategory());
        } finally {
            // 基于 templatePath 向上一层定位 UUID 临时目录并删除
            Path tempUuidDir = skillsPath.getParent();
            if (tempUuidDir != null) {
                FileUtil.del(tempUuidDir.toFile());
            }
        }
        return true;
    }

    /**
     * 执行导入
     * @param repo AgentSkillRepository
     * @param skillNames 确认导入的skill名称
     * @param isCover 是否覆盖
     */
    private void doImport(AgentSkillRepository repo, List<String> skillNames, boolean isCover, String category) {
        List<String> allSkillNames = repo.getAllSkillNames();
        if (skillNames != null && !skillNames.isEmpty()) {
            allSkillNames = allSkillNames.stream().filter(skillNames::contains).toList();
        }

        for (String allSkillName : allSkillNames) {
            SkillPackage skillPackage = buildSkillPackageFromAgentSkill(repo.getSkill(allSkillName), category);
            SkillPackage oldSkillPackage = skillPackageService.getOne(
                    new LambdaQueryWrapper<SkillPackage>()
                            .eq(SkillPackage::getName, allSkillName),
                    false);

            if (oldSkillPackage == null) {
                skillPackageService.save(skillPackage);
            } else if (isCover) {
                skillPackage.setId(oldSkillPackage.getId());
                skillPackageService.updateById(skillPackage);
            }
        }
    }

    /**
     * 基于 AgentSkill  构建 SkillPackage
     * @param agentSkill AgentSkill
     * @return SkillPackage
     */
    private SkillPackage buildSkillPackageFromAgentSkill(AgentSkill agentSkill, String category) {
        SkillPackage skillPackage = new SkillPackage();
        skillPackage.setCategory(category);
        skillPackage.setName(agentSkill.getName());
        skillPackage.setDescription(agentSkill.getDescription());
        skillPackage.setSkillContent(agentSkill.getSkillContent());

        ArrayList<SkillPackageItem> examples = new ArrayList<>();
        ArrayList<SkillPackageItem> references = new ArrayList<>();
        ArrayList<SkillPackageItem> scripts = new ArrayList<>();

        Map<String, String> resources = agentSkill.getResources();
        resources.forEach((path, content) -> {
            if (path.startsWith("examples/")) {
                examples.add(
                        SkillPackageItem
                                .builder()
                                .prefix("examples")
                                .name(path.substring("examples/".length()))
                                .content(content)
                                .build());
            } else if (path.startsWith("references/")) {
                references.add(
                        SkillPackageItem
                                .builder()
                                .prefix("references")
                                .name(path.substring("references/".length()))
                                .content(content)
                                .build());
            } else if (path.startsWith("scripts/")) {
                scripts.add(
                        SkillPackageItem
                                .builder()
                                .prefix("scripts")
                                .name(path.substring("scripts/".length()))
                                .content(content)
                                .build());
            }
        });

        skillPackage.setExamples(JsonUtils.valueToTree(examples));
        skillPackage.setReferences(JsonUtils.valueToTree(references));
        skillPackage.setScripts(JsonUtils.valueToTree(scripts));

        return skillPackage;
    }
}
