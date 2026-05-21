package com.hxh.apboa.skill.imports;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 技能包导入路径解析器，统一本地/Git/压缩包导入时的 skills 根目录定位逻辑。
 */
public final class SkillImportPathResolver {

    private SkillImportPathResolver() {
    }

    /**
     * 解析技能包根目录。
     * <p>支持以下结构：</p>
     * <ul>
     *   <li>{@code skills/技能A/...}</li>
     *   <li>{@code 外层目录/skills/技能A/...}（压缩包多套一层目录时常见）</li>
     *   <li>{@code 技能A/...}（无 skills 子目录时回退到当前层）</li>
     * </ul>
     *
     * @param baseDir 解压或克隆后的根目录
     * @return 技能包根目录
     */
    public static Path resolveSkillsDir(Path baseDir) {
        if (baseDir == null || !Files.isDirectory(baseDir)) {
            throw new BusinessException("技能导入目录无效");
        }

        Path directSkillsDir = baseDir.resolve(SysConst.SKILLS_DIR_NAME);
        if (Files.isDirectory(directSkillsDir)) {
            return directSkillsDir;
        }

        List<Path> subDirectories = listSubDirectories(baseDir);
        if (subDirectories.size() == 1) {
            Path wrapperDir = subDirectories.get(0);
            Path nestedSkillsDir = wrapperDir.resolve(SysConst.SKILLS_DIR_NAME);
            if (Files.isDirectory(nestedSkillsDir)) {
                return nestedSkillsDir;
            }
            return wrapperDir;
        }

        return baseDir;
    }

    /**
     * 解析上传压缩包解压后的 skills 根目录（语义别名，便于 Controller 调用）。
     *
     * @param extractDir 压缩包解压目录（{@code .apboa/temp/{uuid}/}）
     * @return 技能包根目录
     */
    public static Path resolveUploadedSkillsDir(Path extractDir) {
        return resolveSkillsDir(extractDir);
    }

    private static List<Path> listSubDirectories(Path baseDir) {
        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream.filter(Files::isDirectory)
                    .filter(dir -> !SkillImportConstants.isNoiseDirectory(dir.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new BusinessException("读取解压目录失败: " + e.getMessage());
        }
    }
}
