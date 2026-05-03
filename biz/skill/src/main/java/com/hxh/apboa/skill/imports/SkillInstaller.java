package com.hxh.apboa.skill.imports;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.util.FolderUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 描述：技能包安装器，负责技能包目录级别的安装/卸载，支持覆盖/跳过策略
 *
 * @author huxuehao
 **/
@Slf4j
public class SkillInstaller {
    public static final String SKILLS_DIR = SysConst.SKILLS_DIR;

    /**
     * 安装技能包目录到 SKILLS_DIR
     * <ul>
     *   <li>源路径与目标路径相同 → 不复制，返回 true（继续 DB 操作）</li>
     *   <li>目标已存在且 !cover → 返回 false（跳过，不更新 DB）</li>
     *   <li>目标已存在且 cover → 删除旧目录，复制新目录，返回 true</li>
     *   <li>目标不存在 → 复制新目录，返回 true</li>
     * </ul>
     *
     * @param sourceSkillDir 源技能包目录
     * @param skillName      技能包名称
     * @param cover          是否覆盖
     * @return true=已安装或同路径无需复制，false=跳过
     */
    public static boolean install(Path sourceSkillDir, String skillName, boolean cover) {
        Path targetDir = Paths.get(SKILLS_DIR, skillName);

        // 源路径与目标路径相同，无需复制
        if (isSamePath(sourceSkillDir, targetDir)) {
            return true;
        }

        // 目标已存在时的处理
        if (Files.exists(targetDir)) {
            if (!cover) {
                return false;
            }
            // 覆盖模式：先删除旧目录
            FolderUtils.deleteRecursively(targetDir.toAbsolutePath().toString());
        }

        // 复制源目录到目标
        try {
            FolderUtils.copyRecursively(sourceSkillDir.toAbsolutePath().toString(), targetDir.toAbsolutePath().toString());
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * 卸载技能包目录（删除 SKILLS_DIR/{skillName}）
     *
     * @param skillName 技能包名称
     */
    public static void uninstall(String skillName) {
        if (skillName == null || skillName.isEmpty()) {
            return;
        }
        Path targetDir = Paths.get(SKILLS_DIR, skillName);
        if (targetDir.toFile().exists()) {
            FolderUtils.deleteRecursively(targetDir.toAbsolutePath().toString());
        }
    }

    /**
     * 判断两个路径是否指向同一位置
     *
     * @param path1 路径1
     * @param path2 路径2
     * @return 是否相同
     */
    public static boolean isSamePath(Path path1, Path path2) {
        if (path1 == null || path2 == null) {
            return false;
        }
        try {
            return Files.isSameFile(path1.toAbsolutePath().normalize(), path2.toAbsolutePath().normalize());
        } catch (IOException e) {
            // 降级为路径字符串比较
            return path1.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
        }
    }
}
