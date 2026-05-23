package com.hxh.apboa.skill.imports;

import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.enums.SkillFileType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.util.MarkdownSkillParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述：技能包构建器，负责将 AgentSkill 转换为 SkillPackage 实体和 SkillFile 列表
 *
 * @author huxuehao
 **/
public class SkillPackageBuilder {

    /**
     * 构建结果，包含技能包实体和文件列表
     */
    public static class BuildResult {
        private SkillPackage skillPackage;
        private List<SkillFile> skillFiles;

        public BuildResult(SkillPackage skillPackage, List<SkillFile> skillFiles) {
            this.skillPackage = skillPackage;
            this.skillFiles = skillFiles;
        }

        public SkillPackage getSkillPackage() { return skillPackage; }
        public List<SkillFile> getSkillFiles() { return skillFiles; }
    }

    /**
     * 基于 AgentSkill 构建 SkillPackage 实体和 SkillFile 列表
     *
     * @param agentSkill AgentSkill 对象
     * @param category   技能分类
     * @return 构建结果
     */
    public static BuildResult build(AgentSkill agentSkill, String category) {
        SkillPackage skillPackage = new SkillPackage();
        skillPackage.setCategory(category);
        skillPackage.setName(agentSkill.getName());
        skillPackage.setDescription(agentSkill.getDescription());

        List<SkillFile> skillFiles = new ArrayList<>();

        // SKILL.md
        SkillFile skillMd = new SkillFile();
        skillMd.setFileType(SkillFileType.SKILL_MD);
        skillMd.setFileName("SKILL.md");
        skillMd.setFilePath("SKILL.md");
        skillMd.setContent(MarkdownSkillParser.generate(agentSkill.getMetadata(), agentSkill.getSkillContent()));
        skillMd.setSort(0);
        skillFiles.add(skillMd);

        // 拆分资源文件
        Map<String, String> resources = agentSkill.getResources();
        int sort = 0;
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();

            SkillFileType fileType = resolveFileType(path);
            if (fileType == null) {
                continue;
            }

            // 提取文件名
            String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;

            SkillFile sf = new SkillFile();
            sf.setFileType(fileType);
            sf.setFileName(fileName);
            sf.setFilePath(path);
            sf.setContent(content);
            sf.setSort(sort++);
            skillFiles.add(sf);
        }

        return new BuildResult(skillPackage, skillFiles);
    }

    /**
     * 根据资源路径推断文件类型
     */
    private static SkillFileType resolveFileType(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("references/")) {
            return SkillFileType.REFERENCES;
        } else if (normalized.startsWith("examples/")) {
            return SkillFileType.EXAMPLES;
        } else if (normalized.startsWith("scripts/")) {
            return SkillFileType.SCRIPTS;
        }
        return null;
    }
}