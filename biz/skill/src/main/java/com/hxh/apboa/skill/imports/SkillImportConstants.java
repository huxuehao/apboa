package com.hxh.apboa.skill.imports;

/**
 * 技能包导入相关常量。
 */
final class SkillImportConstants {

    static final String SKILL_FILE = "SKILL.md";
    static final String LEGACY_SKILL_FILE = "skill.md";
    static final String MACOS_METADATA_DIR = "__MACOSX";

    private SkillImportConstants() {
    }

    static boolean isNoiseDirectory(String dirName) {
        return dirName.startsWith(".") || MACOS_METADATA_DIR.equals(dirName);
    }
}
