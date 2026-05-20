package com.hxh.apboa.skill.imports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 扫描 skills 根目录，诊断 AgentScope 无法识别技能的原因。
 */
public final class SkillImportInspector {

    private static final Pattern NAME_PATTERN = Pattern.compile("(?m)^name:\\s*(.+?)\\s*$");

    private SkillImportInspector() {
    }

    /**
     * 根据 skills 根目录生成诊断说明。
     */
    public static String buildHint(Path skillsDir) {
        if (skillsDir == null || !Files.isDirectory(skillsDir)) {
            return "skills 目录不存在或不可读: " + skillsDir;
        }

        List<String> issues = new ArrayList<>();
        List<Path> skillDirs = listSkillDirectories(skillsDir);

        if (skillDirs.isEmpty()) {
            issues.add("在 " + skillsDir + " 下未发现技能子目录。");
            issues.addAll(describeTopLevelEntries(skillsDir));
            issues.add("期望结构: skills/技能目录/SKILL.md（SKILL 必须大写，且含 YAML frontmatter 的 name、description）。");
            return String.join("\n", issues);
        }

        for (Path skillDir : skillDirs) {
            String folderName = skillDir.getFileName().toString();
            Path skillFile = skillDir.resolve(SkillImportConstants.SKILL_FILE);
            Path legacyFile = skillDir.resolve(SkillImportConstants.LEGACY_SKILL_FILE);

            if (!Files.exists(skillFile)) {
                if (Files.exists(legacyFile)) {
                    issues.add(folderName + ": 存在 skill.md，需使用 SKILL.md（全大写 SKILL）。");
                } else {
                    issues.add(folderName + ": 缺少 SKILL.md 入口文件。");
                }
                continue;
            }

            try {
                String content = Files.readString(skillFile, StandardCharsets.UTF_8);
                Optional<String> parsedName = parseSkillName(content);
                if (parsedName.isEmpty()) {
                    issues.add(folderName + ": SKILL.md 缺少有效的 YAML frontmatter 字段 name。");
                }
            } catch (IOException e) {
                issues.add(folderName + ": 无法读取 SKILL.md（" + e.getMessage() + "）。");
            }
        }

        if (issues.isEmpty()) {
            return "目录结构看起来正确，但 AgentScope 仍未识别技能，请检查 SKILL.md 内容格式。";
        }

        issues.add(0, "以下技能目录未能被识别：");
        issues.add("""
                SKILL.md 最小示例:
                ---
                name: cust_query
                description: 自定义查询技能
                ---
                技能说明正文...
                """);
        return String.join("\n", issues);
    }

    /**
     * 按 AgentScope 规则，根据 SKILL.md 中的 name 定位技能目录（目录名与 name 可不同）。
     */
    public static Optional<Path> findSkillDirectory(Path skillsDir, String skillName) {
        if (skillsDir == null || skillName == null || skillName.isBlank()) {
            return Optional.empty();
        }
        for (Path skillDir : listSkillDirectories(skillsDir)) {
            Path skillFile = skillDir.resolve(SkillImportConstants.SKILL_FILE);
            if (!Files.isRegularFile(skillFile)) {
                continue;
            }
            try {
                String content = Files.readString(skillFile, StandardCharsets.UTF_8);
                Optional<String> parsedName = parseSkillName(content);
                if (parsedName.isPresent() && skillName.equals(parsedName.get())) {
                    return Optional.of(skillDir);
                }
            } catch (IOException ignored) {
                // skip invalid entries
            }
        }
        Path fallback = skillsDir.resolve(skillName);
        return Files.isDirectory(fallback) ? Optional.of(fallback) : Optional.empty();
    }

    private static List<Path> listSkillDirectories(Path skillsDir) {
        List<Path> directories = new ArrayList<>();
        try (Stream<Path> entries = Files.list(skillsDir)) {
            entries.filter(Files::isDirectory)
                    .filter(dir -> !SkillImportConstants.isNoiseDirectory(dir.getFileName().toString()))
                    .forEach(directories::add);
        } catch (IOException e) {
            return List.of();
        }
        return directories;
    }

    private static List<String> describeTopLevelEntries(Path skillsDir) {
        List<String> lines = new ArrayList<>();
        try (Stream<Path> entries = Files.list(skillsDir)) {
            List<String> names = entries
                    .map(path -> path.getFileName().toString() + (Files.isDirectory(path) ? "/" : ""))
                    .sorted()
                    .toList();
            if (names.isEmpty()) {
                lines.add("当前 skills 目录为空。");
            } else {
                lines.add("当前 skills 目录下仅有: " + String.join(", ", names));
            }
        } catch (IOException e) {
            lines.add("无法读取 skills 目录内容: " + e.getMessage());
        }
        return lines;
    }

    private static Optional<String> parseSkillName(String skillMdContent) {
        if (skillMdContent == null || skillMdContent.isBlank()) {
            return Optional.empty();
        }
        String normalized = skillMdContent.replace("\r\n", "\n");
        if (!normalized.startsWith("---")) {
            return Optional.empty();
        }
        int end = normalized.indexOf("\n---", 3);
        if (end < 0) {
            return Optional.empty();
        }
        String frontmatter = normalized.substring(3, end);
        Matcher matcher = NAME_PATTERN.matcher(frontmatter);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String name = matcher.group(1).trim();
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() >= 2) {
            name = name.substring(1, name.length() - 1);
        }
        if (name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }
}
