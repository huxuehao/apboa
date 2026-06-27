package com.hxh.apboa.core.skill;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.CodeExecutionConfig;
import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.enums.SkillFileType;
import com.hxh.apboa.core.agui.AgentContext;
import com.hxh.apboa.core.workspace.skills.SearchReplaceSkill;
import com.hxh.apboa.core.workspace.skills.WorkspaceSkill;
import com.hxh.apboa.skill.service.AgentSkillPackageService;
import com.hxh.apboa.skill.service.SkillFileService;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.coding.ShellCommandTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 描述：skill 构造器
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class SkillBoxFactory {
    private final SkillPackageService skillPackageService;
    private final SkillFileService skillFileService;
    private final AgentSkillPackageService agentSkillPackageService;

    /**
     * 获取SkillBox
     *
     * @param agentDefinition 智能体定义
     * @param codeExecutionConfig   代码执行配置
     * @return SkillBox
     */
    public SkillBox getSkillBox(AgentDefinition agentDefinition, CodeExecutionConfig codeExecutionConfig) {
        return getSkillBox(agentDefinition, new Toolkit(), codeExecutionConfig);
    }

    /**
     * 获取SkillBox
     *
     * @param agentDefinition 智能体定义
     * @param codeExecutionConfig   代码执行配置
     * @return SkillBox
     */
    public SkillBox getSkillBox(AgentDefinition agentDefinition, Toolkit toolkit, CodeExecutionConfig codeExecutionConfig) {
        SkillBox skillBox = new SkillBox(toolkit);

        configureCodeExecution(skillBox, codeExecutionConfig);

        // 注册技能包
        List<Long> skillPackageIds = agentSkillPackageService.getSkillPackageIds(agentDefinition.getId());
        if (skillPackageIds.isEmpty()) {
            return skillBox;
        }

        registerSkills(skillBox, skillPackageIds);

        return skillBox;
    }

    /**
     * 注册技能包到SkillBox
     *
     * @param skillBox SkillBox
     * @param skillPackageIds 技能包ID列表
     */
    private void registerSkills(SkillBox skillBox, List<Long> skillPackageIds) {
        List<SkillPackage> skillPackages = skillPackageService.listByIds(skillPackageIds);

        skillPackages.stream()
                .filter(SkillPackage::getEnabled)
                .forEach(skillPackage -> registerSkill(skillBox, skillPackage));
    }

    /**
     * 注册单个技能包
     *
     * @param skillBox SkillBox
     * @param skillPackage 技能包
     */
    private void registerSkill(SkillBox skillBox, SkillPackage skillPackage) {
        // 查询技能包的所有入库文件
        List<SkillFile> files = skillFileService.listBySkillId(skillPackage.getId());

        // 查找 SKILL.md 文件
        String skillContent = files.stream()
                .filter(f -> f.getFileType() == SkillFileType.SKILL_MD)
                .map(SkillFile::getContent)
                .findFirst()
                .orElse("");
        List<SkillFile> resourceFiles = files.stream()
                .filter(f -> f.getFileType() != SkillFileType.SKILL_MD)
                .toList();

        AgentSkill baseSkill = AgentSkill.builder()
                .name(skillPackage.getName())
                .description(skillPackage.getDescription())
                .skillContent(skillContent)
                .build();
        AgentSkill.Builder skillBuilder = baseSkill.toBuilder()
                .skillContent(appendResourceUsageHint(baseSkill, resourceFiles));

        // 添加所有资源引用（references/examples/scripts 类型的文件）
        resourceFiles.stream()
                .forEach(f -> skillBuilder.addResource(f.getFilePath(), f.getContent()));

        skillBox.registration().skill(skillBuilder.build()).apply();
    }

    static String appendResourceUsageHint(AgentSkill baseSkill, List<SkillFile> resourceFiles) {
        if (resourceFiles == null || resourceFiles.isEmpty()) {
            return baseSkill.getSkillContent();
        }

        List<String> resourcePaths = resourceFiles.stream()
                .filter(f -> f.getFileType() != SkillFileType.SKILL_MD)
                .map(SkillFile::getFilePath)
                .filter(path -> path != null && !path.isBlank())
                .map(path -> path.replace('\\', '/'))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (resourcePaths.isEmpty()) {
            return baseSkill.getSkillContent();
        }

        String skillContent = baseSkill.getSkillContent();
        StringBuilder hint = new StringBuilder(skillContent == null ? "" : skillContent);
        hint.append("\n\n---\n\n")
                .append("## Skill Resources\n\n")
                .append("When this skill refers to files in this directory, examples/, references/, or scripts/, ")
                .append("treat them as skill resources, not workspace files. Load them with:\n\n")
                .append("`load_skill_through_path(skillId=\"")
                .append(baseSkill.getSkillId())
                .append("\", path=\"<resource-path>\")`\n\n")
                .append("Available resource paths:\n");
        resourcePaths.forEach(path -> hint.append("- `").append(path).append("`\n"));
        return hint.toString();
    }

    /**
     * 配置代码执行环境
     *
     * @param skillBox SkillBox
     * @param config   代码执行配置
     */
    public void configureCodeExecution(SkillBox skillBox, CodeExecutionConfig config) {
        if (skillBox == null || config == null) {
            return;
        }

        // 配置工作空间专属skill
        skillBox.registerSkill(WorkspaceSkill.getAgentSkill());

        // 设置自动上传
        skillBox.setAutoUploadSkill(false);

        // 配置代码执行环境
        SkillBox.CodeExecutionBuilder codeExecutionBuilder = skillBox.codeExecution();

        // 设置工作目录
        codeExecutionBuilder.workDir(SysConst.WORKSPACE_PATH + "/" + AgentContext.get().getThreadId());

        // 配置Shell命令工具
        if (Boolean.TRUE.equals(config.getEnableShell())) {
            Set<String> allowedCommands = parseAllowedCommands(config.getCommand());
            codeExecutionBuilder.withShell(new ShellCommandTool(null, allowedCommands, null));
        }

        // 配置文件读写工具
        if (Boolean.TRUE.equals(config.getEnableRead())) {
            codeExecutionBuilder.withRead();
        }
        if (Boolean.TRUE.equals(config.getEnableWrite())) {
            codeExecutionBuilder.withWrite();
            // 配置工作空间专属skill
            skillBox.registerSkill(SearchReplaceSkill.getAgentSkill());
        }

        codeExecutionBuilder.enable();
    }

    /**
     * 解析允许执行的命令集合
     *
     * @param commandJson 命令JSON节点
     * @return 允许执行的命令集合
     */
    private Set<String> parseAllowedCommands(JsonNode commandJson) {
        Set<String> commands = new HashSet<>();
        if (commandJson == null || commandJson.isEmpty()) {
            return commands;
        }
        if (commandJson.isArray()) {
            commandJson.forEach(node -> commands.add(node.asText()));
        }
        return commands;
    }
}
