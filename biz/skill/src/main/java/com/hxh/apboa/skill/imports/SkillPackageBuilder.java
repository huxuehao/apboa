package com.hxh.apboa.skill.imports;

import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.util.JsonUtils;
import io.agentscope.core.skill.AgentSkill;

import java.util.ArrayList;
import java.util.Map;

/**
 * 描述：技能包构建器，负责将 AgentSkill 转换为 SkillPackage 实体
 *
 * @author huxuehao
 **/
public class SkillPackageBuilder {

    /**
     * 基于 AgentSkill 构建 SkillPackage 实体
     *
     * @param agentSkill AgentSkill 对象
     * @param category   技能分类
     * @return SkillPackage 实体
     */
    public static SkillPackage build(AgentSkill agentSkill, String category) {
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
