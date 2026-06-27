package com.hxh.apboa.core.skill;

import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.enums.SkillFileType;
import io.agentscope.core.skill.AgentSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillBoxFactoryTest {

    @Test
    void appendResourceUsageHintTellsAgentToLoadResourcesThroughSkillTool() {
        SkillFile example = skillFile(SkillFileType.EXAMPLES, "examples/root-cause.md");
        SkillFile reference = skillFile(SkillFileType.REFERENCES, "references/patterns.md");
        SkillFile skillMd = skillFile(SkillFileType.SKILL_MD, "SKILL.md");
        AgentSkill baseSkill = AgentSkill.builder()
                .name("debugging")
                .description("Debugging")
                .source("repo")
                .skillContent("# Debugging\nSee `examples/root-cause.md` in this directory.")
                .build();

        String content = SkillBoxFactory.appendResourceUsageHint(baseSkill, List.of(example, reference, skillMd));

        assertTrue(content.contains("load_skill_through_path(skillId=\"debugging_repo\", path=\"<resource-path>\")"));
        assertTrue(content.contains("- `examples/root-cause.md`"));
        assertTrue(content.contains("- `references/patterns.md`"));
        assertFalse(content.contains("- `SKILL.md`"));
        assertTrue(content.contains("not workspace files"));
    }

    @Test
    void appendResourceUsageHintLeavesContentUnchangedWhenNoResourcesExist() {
        AgentSkill baseSkill = AgentSkill.builder()
                .name("plain")
                .description("Plain")
                .skillContent("# Plain skill")
                .build();

        assertEquals("# Plain skill", SkillBoxFactory.appendResourceUsageHint(baseSkill, List.of()));
    }

    private static SkillFile skillFile(SkillFileType fileType, String filePath) {
        SkillFile file = new SkillFile();
        file.setFileType(fileType);
        file.setFilePath(filePath);
        return file;
    }
}
