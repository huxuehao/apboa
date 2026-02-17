package com.hxh.apboa.core.skill;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.key.SkillExampleKey;
import com.hxh.apboa.common.key.SkillReferencesKey;
import com.hxh.apboa.common.key.SkillScriptKey;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 描述：skill 构造器
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class SkillBoxFactory {
    private final SkillPackageService skillPackageService;
    private final AgentSkillPackageService agentSkillPackageService;

    public SkillBox getSkillBox(AgentDefinition agentDefinition) {
        SkillBox skillBox = new SkillBox(new Toolkit());

        List<Long> skillPackageIds = agentSkillPackageService.getSkillPackageIds(agentDefinition.getId());
        if (skillPackageIds.isEmpty()) {
            return null;
        }

        // 查询技能包集合
        List<SkillPackage> skillPackages = skillPackageService.listByIds(skillPackageIds);

        // 过滤可用的技能包
        skillPackages.stream().filter(SkillPackage::getEnabled).forEach(skillPackage -> {
            AgentSkill.Builder skillBuilder = AgentSkill.builder()
                    .name(skillPackage.getName())
                    .description(skillPackage.getDescription())
                    .skillContent(skillPackage.getSkillContent());

            // 添加资源
            JsonNode references = skillPackage.getReferences();
            if (skillPackage.getReferences() != null && !skillPackage.getReferences().isEmpty()) {
                references.forEach(resource -> {
                    String prefix = resource.get(SkillReferencesKey.prefix).asText();
                    String name = resource.get(SkillReferencesKey.name).asText();
                    String content = resource.get(SkillReferencesKey.content).asText();
                    skillBuilder.addResource(String.format("%s/%s", prefix, name), content);
                });
            }

            // 添加示例
            JsonNode examples = skillPackage.getExamples();
            if (skillPackage.getExamples() != null && !skillPackage.getExamples().isEmpty()) {
                examples.forEach(resource -> {
                    String prefix = resource.get(SkillExampleKey.prefix).asText();
                    String name = resource.get(SkillExampleKey.name).asText();
                    String content = resource.get(SkillExampleKey.content).asText();
                    skillBuilder.addResource(String.format("%s/%s", prefix, name), content);
                });
            }

            // 添加脚本
            JsonNode scripts = skillPackage.getScripts();
            if (skillPackage.getScripts() != null && !skillPackage.getScripts().isEmpty()) {
                scripts.forEach(resource -> {
                    String prefix = resource.get(SkillScriptKey.prefix).asText();
                    String name = resource.get(SkillScriptKey.name).asText();
                    String content = resource.get(SkillScriptKey.content).asText();
                    skillBuilder.addResource(String.format("%s/%s", prefix, name), content);
                });
            }

            skillBox.registerSkill(skillBuilder.build());
        });

        return skillBox;
    }
}
