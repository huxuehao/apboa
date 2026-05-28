package com.hxh.apboa.core.skill;

import com.hxh.apboa.agent.service.AgentCodeExecutionService;
import com.hxh.apboa.agent.service.CodeExecutionConfigService;
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
    private final AgentCodeExecutionService agentCodeExecutionService;
    private final CodeExecutionConfigService codeExecutionConfigService;

    /**
     * 获取SkillBox
     *
     * @param agentDefinition 智能体定义
     * @return SkillBox
     */
    public SkillBox getSkillBox(AgentDefinition agentDefinition) {
        return getSkillBox(agentDefinition, new Toolkit());
    }

    /**
     * 获取SkillBox
     *
     * @param agentDefinition 智能体定义
     * @return SkillBox
     */
    public SkillBox getSkillBox(AgentDefinition agentDefinition, Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);

        // 注册技能包
        List<Long> skillPackageIds = agentSkillPackageService.getSkillPackageIds(agentDefinition.getId());
        if (skillPackageIds.isEmpty()) {
            return skillBox;
        }

        registerSkills(skillBox, skillPackageIds);

        configureCodeExecution(skillBox, agentDefinition.getId());

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

        AgentSkill.Builder skillBuilder = AgentSkill.builder()
                .name(skillPackage.getName())
                .description(skillPackage.getDescription())
                .skillContent(skillContent);

        // 添加所有资源引用（references/examples/scripts 类型的文件）
        files.stream()
                .filter(f -> f.getFileType() != SkillFileType.SKILL_MD)
                .forEach(f -> skillBuilder.addResource(f.getFilePath(), f.getContent()));

        skillBox.registration().skill(skillBuilder.build()).apply();
    }

    /**
     * 配置代码执行环境
     *
     * @param skillBox SkillBox
     * @param agentDefinitionId 智能体定义ID
     */
    public void configureCodeExecution(SkillBox skillBox, Long agentDefinitionId) {
        if (skillBox == null) {
            return;
        }
        // 获取代码执行配置
        CodeExecutionConfig config = getCodeExecutionConfig(agentDefinitionId);
        if (config == null) {
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
     * 获取代码执行配置
     *
     * @param agentDefinitionId 智能体定义ID
     * @return 代码执行配置
     */
    private CodeExecutionConfig getCodeExecutionConfig(Long agentDefinitionId) {
        Long codeExecutionId = agentCodeExecutionService.getCodeExecutionIdByAgentId(agentDefinitionId);
        if (codeExecutionId == null) {
            return null;
        }
        return codeExecutionConfigService.getById(codeExecutionId);
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
