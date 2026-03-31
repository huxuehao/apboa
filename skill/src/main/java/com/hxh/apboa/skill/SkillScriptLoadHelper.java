package com.hxh.apboa.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 描述：技能包执行脚本装载工具类
 *
 * @author huxuehao
 **/
@Slf4j
public class SkillScriptLoadHelper {
    public static final String BASE_DIR = ".apboa/skill-scripts/skills";
    private static final String SCRIPTS_SUB_DIR = "scripts";

    /**
     * 装载技能包脚本
     *
     * @param skillPackage 技能包
     */
    public static void loadScripts(SkillPackage skillPackage) {
        if (skillPackage == null || isScriptsEmpty(skillPackage.getScripts())) {
            return;
        }

        Path targetDir = createTargetDirectory(skillPackage.getName());
        if (targetDir == null) {
            return;
        }

        List<ScriptItem> scriptItems = parseScriptItems(skillPackage.getScripts());
        for (ScriptItem item : scriptItems) {
            writeScriptFile(targetDir, item);
        }
    }

    /**
     * 移除技能包脚本目录
     * 当scripts为null或空数组时，删除已存在的脚本目录
     *
     * @param skillPackage 技能包
     */
    public static void removeScripts(SkillPackage skillPackage) {
        if (skillPackage == null) {
            return;
        }

        // scripts不为空时不处理
        if (!isScriptsEmpty(skillPackage.getScripts())) {
            return;
        }

        Path skillDir = getSkillDirectory(skillPackage.getName());
        if (skillDir != null && Files.exists(skillDir)) {
            deleteDirectory(skillDir);
            log.info("删除技能包脚本目录: {}", skillDir.toAbsolutePath());
        }
    }

    /**
     * 获取技能包脚本根目录
     *
     * @param skillName 技能包名称
     * @return 脚本根目录路径
     */
    private static Path getSkillDirectory(String skillName) {
        if (skillName == null || skillName.isEmpty()) {
            return null;
        }
        return Paths.get(BASE_DIR, skillName);
    }

    /**
     * 删除目录及其所有内容
     *
     * @param directory 目标目录
     */
    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((a, b) -> -a.compareTo(b)) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path.toAbsolutePath(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("删除目录失败: {}", directory.toAbsolutePath(), e);
        }
    }

    /**
     * 检查脚本列表是否为空
     *
     * @param scripts 脚本JSON节点
     * @return 是否为空
     */
    private static boolean isScriptsEmpty(JsonNode scripts) {
        if (scripts == null || scripts.isNull()) {
            return true;
        }
        if (!scripts.isArray()) {
            return true;
        }
        return scripts.isEmpty();
    }

    /**
     * 创建目标目录（如果存在则先清理旧文件）
     *
     * @param skillName 技能包名称
     * @return 目标目录路径，创建失败返回null
     */
    private static Path createTargetDirectory(String skillName) {
        Path targetDir = Paths.get(BASE_DIR, skillName, SCRIPTS_SUB_DIR);
        try {
            // 如果目录已存在，先清理旧文件以支持更新操作
            if (Files.exists(targetDir)) {
                cleanDirectory(targetDir);
                log.info("清理旧脚本目录: {}", targetDir.toAbsolutePath());
            }
            // 创建目录
            Files.createDirectories(targetDir);
            log.info("创建脚本目录: {}", targetDir.toAbsolutePath());
            return targetDir;
        } catch (IOException e) {
            log.error("创建脚本目录失败: {}", targetDir.toAbsolutePath(), e);
            return null;
        }
    }

    /**
     * 清理目录下的所有文件和子目录
     *
     * @param directory 目标目录
     * @throws IOException IO异常
     */
    private static void cleanDirectory(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((a, b) -> -a.compareTo(b)) // 先删除文件，再删除目录
                    .filter(path -> !path.equals(directory)) // 不删除根目录本身
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path.toAbsolutePath(), e);
                        }
                    });
        }
    }

    /**
     * 解析脚本项列表
     *
     * @param scripts 脚本JSON节点
     * @return 脚本项列表
     */
    private static List<ScriptItem> parseScriptItems(JsonNode scripts) {
        List<ScriptItem> items = new ArrayList<>();
        for (JsonNode node : scripts) {
            String name = JsonUtils.getStringValue(node, "name", null);
            String content = JsonUtils.getStringValue(node, "content", "");

            if (name == null || name.isEmpty()) {
                log.warn("脚本项缺少name字段，跳过");
                continue;
            }

            items.add(new ScriptItem(name, content));
        }
        return items;
    }

    /**
     * 写入脚本文件
     *
     * @param targetDir 目标目录
     * @param item      脚本项
     */
    private static void writeScriptFile(Path targetDir, ScriptItem item) {
        Path filePath = resolveFilePath(targetDir, item.name);
        if (filePath == null) {
            return;
        }

        try {
            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 写入文件内容
            Files.writeString(filePath, item.content, StandardCharsets.UTF_8);
            log.info("写入脚本文件: {}", filePath.toAbsolutePath());

            // 设置文件权限
            setFilePermissions(filePath);
        } catch (IOException e) {
            log.error("写入脚本文件失败: {}", filePath.toAbsolutePath(), e);
        }
    }

    /**
     * 解析文件路径，处理可能包含子目录的文件名
     *
     * @param targetDir 目标目录
     * @param fileName  文件名（可能包含子目录，如 "tool/test.py"）
     * @return 完整文件路径
     */
    private static Path resolveFilePath(Path targetDir, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 处理路径分隔符，统一使用当前系统的分隔符
        String normalizedFileName = fileName.replace('/', File.separatorChar).replace('\\', File.separatorChar);
        return targetDir.resolve(normalizedFileName);
    }

    /**
     * 设置文件权限为读写执行
     * 兼容Windows和Unix-like系统
     *
     * @param filePath 文件路径
     */
    private static void setFilePermissions(Path filePath) {
//        try {
//            // 设置文件可读写
//            filePath.toFile().setReadable(true, false);
//            filePath.toFile().setWritable(true, false);
//
//            // 在Unix-like系统上设置执行权限
//            if (!isWindows()) {
//                setUnixFilePermissions(filePath);
//            } else {
//                // Windows系统通过设置可执行属性
//                filePath.toFile().setExecutable(true, false);
//            }
//        } catch (Exception e) {
//            log.warn("设置文件权限失败: {}", filePath.toAbsolutePath(), e);
//        }
    }

    /**
     * 判断当前系统是否为Windows
     *
     * @return 是否为Windows系统
     */
    private static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("windows");
    }

    /**
     * 在Unix-like系统上设置文件权限
     *
     * @param filePath 文件路径
     * @throws IOException IO异常
     */
    private static void setUnixFilePermissions(Path filePath) throws IOException {
        Set<PosixFilePermission> permissions = new HashSet<>();
        // 所有者权限：读、写、执行
        permissions.add(PosixFilePermission.OWNER_READ);
        permissions.add(PosixFilePermission.OWNER_WRITE);
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        // 组权限：读、执行
        permissions.add(PosixFilePermission.GROUP_READ);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        // 其他用户权限：读、执行
        permissions.add(PosixFilePermission.OTHERS_READ);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        try {
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (UnsupportedOperationException e) {
            // 某些文件系统不支持POSIX权限，降级处理
            filePath.toFile().setExecutable(true, false);
        }
    }

    /**
     * 脚本项内部类
     */
    private static class ScriptItem {
        private final String name;
        private final String content;

        public ScriptItem(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }
}
