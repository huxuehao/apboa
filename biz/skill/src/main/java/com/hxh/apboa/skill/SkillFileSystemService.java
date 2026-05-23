package com.hxh.apboa.skill;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.enums.SkillFileType;
import com.hxh.apboa.common.util.FolderUtils;
import com.hxh.apboa.params.core.ParamsAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 描述：技能包文件系统操作服务
 *
 * @author huxuehao
 **/
@Slf4j
public class SkillFileSystemService {

    private static final String BASE_DIR = SysConst.SKILLS_DIR;

    /** 需要入库的目录前缀 */
    private static final List<String> DB_DIR_PREFIXES = List.of("references/", "examples/", "scripts/");

    /** SKILL.md 文件名 */
    private static final String SKILL_MD_NAME = "SKILL.md";

    /** Params 系统中的参数 key */
    private static final String EXT_PARAMS_KEY = "SKILL_FILE_ALLOWED_EXTENSIONS";

    /** 默认允许入库的文件扩展名（Params 未配置时兜底） */
    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
        "md", "py", "sh", "js", "ts", "json", "yaml", "yml", "xml", "txt",
        "java", "cs", "go", "rs", "rb", "php", "sql", "html", "css", "scss", "less", "cfg", "conf", "toml"
    );

    /** ParamsAdapter（由 Spring 桥接注入） */
    private static volatile ParamsAdapter paramsAdapter;

    /** 缓存的扩展名白名单 */
    private static volatile Set<String> cachedExtensions;

    /**
     * 由 Spring 桥接组件注入 ParamsAdapter
     */
    static void setParamsAdapter(ParamsAdapter adapter) {
        paramsAdapter = adapter;
    }

    /**
     * 清除扩展名白名单缓存
     * 由事件监听器调用，当 Params 系统中的 SKILL_FILE_ALLOWED_EXTENSIONS 参数更新时触发
     */
    public static void clearExtensionCache() {
        cachedExtensions = null;
        log.info("技能文件扩展名白名单缓存已清除");
    }

    /**
     * 从 Params 表读取白名单，带缓存
     */
    private static Set<String> loadAllowedExtensions() {
        // 优先使用缓存
        Set<String> cached = cachedExtensions;
        if (cached != null) {
            return cached;
        }
        // 尝试从 ParamsAdapter 加载
        if (paramsAdapter != null) {
            String value = paramsAdapter.getValue(EXT_PARAMS_KEY);
            if (value != null && !value.isEmpty()) {
                Set<String> result = Arrays.stream(value.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
                cachedExtensions = result;
                return result;
            }
        }
        // 兜底
        return DEFAULT_ALLOWED_EXTENSIONS;
    }

    /**
     * 判断文件是否需要入库
     *
     * @param relativePath 相对于技能包根目录的路径
     * @return 是否需要入库
     */
    public static boolean shouldPersistToDb(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        // 规范化路径分隔符
        String normalizedPath = relativePath.replace('\\', '/');

        // 根目录的 SKILL.md
        if (SKILL_MD_NAME.equals(normalizedPath)) {
            return true;
        }

        // references/、examples/、scripts/ 下的文件
        for (String prefix : DB_DIR_PREFIXES) {
            if (normalizedPath.startsWith(prefix)) {
                // 还需检查扩展名是否在白名单内
                return isAllowedExtension(normalizedPath);
            }
        }

        return false;
    }

    /**
     * 检查文件扩展名是否在白名单内
     *
     * @param path 文件路径
     * @return 是否允许入库
     */
    private static boolean isAllowedExtension(String path) {
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx < 0) {
            return false;
        }
        String ext = path.substring(dotIdx + 1).toLowerCase();
        return loadAllowedExtensions().contains(ext);
    }

    /**
     * 获取所有允许入库的文件扩展名
     *
     * @return 扩展名列表
     */
    public static List<String> getAllowedExtensions() {
        return new ArrayList<>(loadAllowedExtensions());
    }

    /**
     * 根据路径推断 file_type
     *
     * @param relativePath 相对路径
     * @return 文件类型枚举
     */
    public static SkillFileType resolveFileType(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        String normalizedPath = relativePath.replace('\\', '/');

        if (SKILL_MD_NAME.equals(normalizedPath)) {
            return SkillFileType.SKILL_MD;
        }
        if (normalizedPath.startsWith("references/")) {
            return SkillFileType.REFERENCES;
        }
        if (normalizedPath.startsWith("examples/")) {
            return SkillFileType.EXAMPLES;
        }
        if (normalizedPath.startsWith("scripts/")) {
            return SkillFileType.SCRIPTS;
        }

        return null;
    }

    /**
     * 获取技能包根目录路径
     *
     * @param skillName 技能包名称
     * @return 根目录 Path
     */
    public static Path getSkillDirPath(String skillName) {
        return Paths.get(BASE_DIR, skillName);
    }

    /**
     * 创建技能包根目录
     *
     * @param skillName 技能包名称
     * @return 根目录 Path
     */
    public static Path buildSkillDir(String skillName) {
        Path skillDir = getSkillDirPath(skillName);
        FolderUtils.mkdirsByAbsolutePath(skillDir.toAbsolutePath().toString());
        return skillDir;
    }

    /**
     * 写入文件到技能包目录下的指定路径
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @param content      文件内容
     * @return 成功返回 true
     */
    public static boolean writeFile(String skillName, String relativePath, String content) {
        try {
            Path skillDir = buildSkillDir(skillName);
            Path filePath = skillDir.resolve(relativePath);

            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.writeString(filePath, content != null ? content : "", StandardCharsets.UTF_8);
            log.debug("写入技能文件: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("写入技能文件失败: skillName={}, path={}", skillName, relativePath, e);
            return false;
        }
    }

    /**
     * 以原始字节写入文件到技能包目录（用于二进制文件）
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @param bytes        原始字节
     * @return 成功返回 true
     */
    public static boolean writeFileBytes(String skillName, String relativePath, byte[] bytes) {
        try {
            Path skillDir = buildSkillDir(skillName);
            Path filePath = skillDir.resolve(relativePath);

            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            Files.write(filePath, bytes);
            log.debug("写入技能二进制文件: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("写入技能二进制文件失败: skillName={}, path={}", skillName, relativePath, e);
            return false;
        }
    }

    /**
     * 删除文件系统中的技能文件
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @return 成功返回 true
     */
    public static boolean deleteFile(String skillName, String relativePath) {
        try {
            Path skillDir = getSkillDirPath(skillName);
            Path filePath = skillDir.resolve(relativePath);
            Files.deleteIfExists(filePath);
            log.debug("删除技能文件: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            log.error("删除技能文件失败: skillName={}, path={}", skillName, relativePath, e);
            return false;
        }
    }

    /**
     * 递归删除文件系统中的目录
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @return 成功返回 true
     */
    public static boolean deleteDirectory(String skillName, String relativePath) {
        Path skillDir = getSkillDirPath(skillName);
        Path dirPath = skillDir.resolve(relativePath);
        return FolderUtils.deleteRecursively(dirPath.toAbsolutePath().toString());
    }

    /**
     * 创建目录
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @return 成功返回 true
     */
    public static boolean createDirectory(String skillName, String relativePath) {
        Path skillDir = buildSkillDir(skillName);
        Path dirPath = skillDir.resolve(relativePath);
        FolderUtils.mkdirsByAbsolutePath(dirPath.toAbsolutePath().toString());
        return true;
    }

    /**
     * 删除整个技能包目录
     *
     * @param skillName 技能包名称
     * @return 成功返回 true
     */
    public static boolean removeSkillDir(String skillName) {
        Path skillDir = getSkillDirPath(skillName);
        if (Files.exists(skillDir)) {
            boolean result = FolderUtils.deleteRecursively(skillDir.toAbsolutePath().toString());
            log.info("删除技能包目录: {}, 结果: {}", skillDir.toAbsolutePath(), result);
            return result;
        }
        return true;
    }

    /**
     * 扫描技能包文件树
     *
     * @param skillName 技能包名称
     * @return 文件树节点列表
     */
    public static List<FileTreeNode> scanSkillTree(String skillName) {
        Path skillDir = getSkillDirPath(skillName);
        if (!Files.exists(skillDir)) {
            return Collections.emptyList();
        }

        Map<String, FileTreeNode> dirMap = new LinkedHashMap<>();
        List<FileTreeNode> rootNodes = new ArrayList<>();

        try {
            Files.walkFileTree(skillDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(skillDir)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String relPath = skillDir.relativize(dir).toString().replace('\\', '/');
                    FileTreeNode node = new FileTreeNode();
                    node.setName(dir.getFileName().toString());
                    node.setPath(relPath);
                    node.setDirectory(true);
                    node.setExtension("");

                    // 查找父节点并添加到其 children
                    String parentPath = getParentPath(relPath);
                    FileTreeNode parentNode = dirMap.get(parentPath);
                    if (parentNode != null) {
                        parentNode.getChildren().add(node);
                    } else {
                        rootNodes.add(node);
                    }
                    dirMap.put(relPath, node);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relPath = skillDir.relativize(file).toString().replace('\\', '/');
                    FileTreeNode node = new FileTreeNode();
                    node.setName(file.getFileName().toString());
                    node.setPath(relPath);
                    node.setDirectory(false);
                    node.setExtension(getExtension(file.getFileName().toString()));
                    node.setFileSize(attrs.size());

                    String parentPath = getParentPath(relPath);
                    FileTreeNode parentNode = dirMap.get(parentPath);
                    if (parentNode != null) {
                        parentNode.getChildren().add(node);
                    } else {
                        rootNodes.add(node);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("扫描技能包文件树失败: skillName={}", skillName, e);
        }

        // 排序：目录在前，文件在后，按名称排序
        sortTreeNodes(rootNodes);

        return rootNodes;
    }

    /**
     * 读取文件系统中的文件内容
     *
     * @param skillName    技能包名称
     * @param relativePath 相对路径
     * @return 文件内容，文件不存在返回 null
     */
    public static String readFileContent(String skillName, String relativePath) {
        try {
            Path skillDir = getSkillDirPath(skillName);
            Path filePath = skillDir.resolve(relativePath);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取技能文件内容失败: skillName={}, path={}", skillName, relativePath, e);
            return null;
        }
    }

    /**
     * 递归排序树节点
     */
    private static void sortTreeNodes(List<FileTreeNode> nodes) {
        nodes.sort(Comparator.comparing(FileTreeNode::isDirectory).reversed()
                .thenComparing(FileTreeNode::getName, String.CASE_INSENSITIVE_ORDER));
        for (FileTreeNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTreeNodes(node.getChildren());
            }
        }
    }

    /**
     * 获取父路径（用于构建树结构）
     */
    private static String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return path.substring(0, lastSlash);
    }

    /**
     * 获取文件扩展名
     */
    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 解析 SKILL.md 中的 YAML 头，提取 name 和 description
     *
     * @param content 文件内容
     * @return 解析结果，name / description 字段已在 Map 中
     */
    public static Map<String, String> parseSkillMdHeader(String content) {
        Map<String, String> result = new HashMap<>();
        if (content == null || content.isEmpty()) {
            return result;
        }

        String[] lines = content.split("\\n");
        boolean inHeader = false;
        int separatorCount = 0;
        for (String line : lines) {
            if ("---".equals(line.trim())) {
                separatorCount++;
                if (separatorCount == 1) {
                    inHeader = true;
                    continue;
                } else if (inHeader) {
                    break;
                }
            }
            if (inHeader) {
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    String key = line.substring(0, colonIdx).trim();
                    String value = line.substring(colonIdx + 1).trim();
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * 构建 SKILL.md 初始内容（带 YAML 头）
     *
     * @param name        技能包名称
     * @param description 描述
     * @return 带 YAML 头的 SKILL.md 内容
     */
    public static String buildSkillMdContent(String name, String description) {
        return "---\n" +
                "name: " + (name != null ? name : "") + "\n" +
                "description: " + (description != null ? description : "") + "\n" +
                "---\n";
    }

    /**
     * 文件树节点（内部用）
     */
    public static class FileTreeNode {
        private String name;
        private String path;
        private boolean directory;
        private String extension;
        private long fileSize;
        private List<FileTreeNode> children = new ArrayList<>();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isDirectory() { return directory; }
        public void setDirectory(boolean directory) { this.directory = directory; }
        public String getExtension() { return extension; }
        public void setExtension(String extension) { this.extension = extension; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public List<FileTreeNode> getChildren() { return children; }
        public void setChildren(List<FileTreeNode> children) { this.children = children; }
    }
}
