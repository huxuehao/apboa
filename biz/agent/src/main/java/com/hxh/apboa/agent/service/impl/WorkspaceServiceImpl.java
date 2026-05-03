package com.hxh.apboa.agent.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.hxh.apboa.agent.service.WorkspaceService;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.exception.BusinessException;
import com.hxh.apboa.common.vo.WorkspaceFileNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 描述：工作空间文件管理 Service 实现
 *
 * @author huxuehao
 **/
@Slf4j
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Override
    public String uploadFile(String sessionId, MultipartFile file) {
        validateSessionId(sessionId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        Path workspaceDir = getWorkspaceDir(sessionId);
        String originalFilename = file.getOriginalFilename();
        Path targetPath = workspaceDir.resolve(originalFilename).normalize();

        // 安全校验：确保目标路径在工作空间内
        validatePathInsideWorkspace(targetPath, workspaceDir);

        // 确保父目录存在
        FileUtil.mkdir(targetPath.getParent().toFile());

        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("文件上传失败: sessionId={}, file={}", sessionId, originalFilename, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        return workspaceDir.relativize(targetPath).toString().replace('\\', '/');
    }

    @Override
    public List<String> uploadFiles(String sessionId, MultipartFile[] files) {
        validateSessionId(sessionId);
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }

        List<String> paths = new ArrayList<>();
        for (MultipartFile file : files) {
            paths.add(uploadFile(sessionId, file));
        }
        return paths;
    }

    @Override
    public List<String> uploadAndExtractArchive(String sessionId, MultipartFile file) {
        validateSessionId(sessionId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传压缩包不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (!isArchiveFile(originalFilename)) {
            throw new BusinessException("不支持的压缩包格式，仅支持 zip");
        }

        Path workspaceDir = getWorkspaceDir(sessionId);
        Path tempFile = null;

        try {
            // 保存到临时文件
            tempFile = Files.createTempFile("workspace_upload_", getArchiveSuffix(originalFilename));
            file.transferTo(tempFile.toFile());

            // 使用 JDK 原生方式解压 ZIP，逐条校验路径防 Zip Slip
            extractZipSafely(tempFile, workspaceDir);

            // 收集解压后的文件相对路径
            return listRelativePaths(workspaceDir);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("压缩包解压失败: sessionId={}, file={}", sessionId, originalFilename, e);
            throw new BusinessException("压缩包解压失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                FileUtil.del(tempFile.toFile());
            }
        }
    }

    @Override
    public List<WorkspaceFileNode> listFiles(String sessionId) {
        Path workspaceDir = getWorkspaceDir(sessionId);
        if (!Files.exists(workspaceDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> walk = Files.list(workspaceDir)) {
            return walk
                    .map(path -> buildFileNode(path, workspaceDir))
                    .sorted((a, b) -> {
                        // 目录排在前面，文件排在后面；同类按名称排序
                        if (a.isDirectory() != b.isDirectory()) {
                            return a.isDirectory() ? -1 : 1;
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .toList();
        } catch (IOException e) {
            log.error("获取工作空间文件列表失败: sessionId={}", sessionId, e);
            throw new BusinessException("获取文件列表失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadFile(String sessionId, String fileName, OutputStream outputStream) {
        Path workspaceDir = getWorkspaceDir(sessionId);
        Path resolvedPath = resolveAndValidate(workspaceDir, fileName);

        if (!Files.exists(resolvedPath)) {
            throw new BusinessException("文件不存在: " + fileName);
        }
        if (Files.isDirectory(resolvedPath)) {
            throw new BusinessException("路径是目录，不支持下载: " + fileName);
        }

        try (InputStream inputStream = Files.newInputStream(resolvedPath)) {
            IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            log.error("文件下载失败: sessionId={}, fileName={}", sessionId, fileName, e);
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadFiles(String sessionId, List<String> filePaths, OutputStream outputStream) {
        Path workspaceDir = getWorkspaceDir(sessionId);

        // 校验所有路径并收集文件
        List<Path> validFiles = new ArrayList<>();
        for (String filePath : filePaths) {
            Path resolvedPath = resolveAndValidate(workspaceDir, filePath);
            if (!Files.exists(resolvedPath)) {
                throw new BusinessException("文件不存在: " + filePath);
            }
            if (Files.isDirectory(resolvedPath)) {
                throw new BusinessException("路径是目录，不支持下载: " + filePath);
            }
            validFiles.add(resolvedPath);
        }

        // 打包成 ZIP 写入输出流
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Path file : validFiles) {
                String entryName = workspaceDir.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                try (InputStream inputStream = Files.newInputStream(file)) {
                    IoUtil.copy(inputStream, zos);
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            log.error("批量下载打包失败: sessionId={}", sessionId, e);
            throw new BusinessException("批量下载打包失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadWorkspace(String sessionId, OutputStream outputStream) {
        Path workspaceDir = getWorkspaceDir(sessionId);

        if (!Files.exists(workspaceDir)) {
            throw new BusinessException("工作空间不存在");
        }

        // 递归打包工作空间所有文件
        try (ZipOutputStream zos = new ZipOutputStream(outputStream);
             Stream<Path> walk = Files.walk(workspaceDir)) {
            walk.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        String entryName = workspaceDir.relativize(path).toString().replace('\\', '/');
                        try {
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("打包文件失败: " + entryName, e);
                        }
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BusinessException(e.getMessage());
        } catch (IOException e) {
            log.error("工作空间下载打包失败: sessionId={}", sessionId, e);
            throw new BusinessException("工作空间下载打包失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String sessionId, String filePath) {
        Path workspaceDir = getWorkspaceDir(sessionId);
        Path resolvedPath = resolveAndValidate(workspaceDir, filePath);

        if (!Files.exists(resolvedPath)) {
            throw new BusinessException("文件不存在: " + filePath);
        }

        try {
            // 如果是目录则递归删除，否则删除单文件
            if (Files.isDirectory(resolvedPath)) {
                FileUtil.del(resolvedPath.toFile());
            } else {
                Files.delete(resolvedPath);
            }
        } catch (IOException e) {
            log.error("文件删除失败: sessionId={}, filePath={}", sessionId, filePath, e);
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }
    }

    @Override
    public void clearWorkspace(String sessionId) {
        Path workspaceDir = getWorkspaceDir(sessionId);

        if (!Files.exists(workspaceDir)) {
            return;
        }

        // 清空目录内容但保留根目录
        try (Stream<Path> walk = Files.list(workspaceDir)) {
            walk.forEach(path -> FileUtil.del(path.toFile()));
        } catch (IOException e) {
            log.error("清空工作空间失败: sessionId={}", sessionId, e);
            throw new BusinessException("清空工作空间失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取工作空间目录路径，若不存在则创建
     */
    private Path getWorkspaceDir(String sessionId) {
        validateSessionId(sessionId);
        Path dir = Paths.get(SysConst.WORKSPACE_PATH, sessionId).toAbsolutePath().normalize();
        FileUtil.mkdir(dir.toFile());
        return dir;
    }

    /**
     * 校验 sessionId 合法性，防止路径注入
     */
    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException("sessionId 不能为空");
        }
        // 禁止路径遍历字符
        if (sessionId.contains("..") || sessionId.contains("/") || sessionId.contains("\\") || sessionId.contains(File.separator)) {
            throw new BusinessException("sessionId 包含非法字符");
        }
    }

    /**
     * 解析并校验相对路径，确保解析后的绝对路径在工作空间目录内
     *
     * @param workspaceDir 工作空间目录
     * @param relativePath 相对路径
     * @return 规范化后的绝对路径
     */
    private Path resolveAndValidate(Path workspaceDir, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("文件路径不能为空");
        }

        Path resolved = workspaceDir.resolve(relativePath).normalize();
        validatePathInsideWorkspace(resolved, workspaceDir);
        return resolved;
    }

    /**
     * 校验目标路径是否在工作空间内，防止路径遍历攻击
     */
    private void validatePathInsideWorkspace(Path targetPath, Path workspaceDir) {
        if (!targetPath.startsWith(workspaceDir)) {
            throw new BusinessException("非法的文件路径，不允许越界访问");
        }
    }

    /**
     * 判断文件名是否为支持的压缩包格式
     */
    private boolean isArchiveFile(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".zip");
    }

    /**
     * 获取压缩包后缀，用于临时文件命名
     */
    private String getArchiveSuffix(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".tar.gz")) return ".tar.gz";
        if (lower.endsWith(".tgz")) return ".tgz";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".zip";
    }

    /**
     * 安全解压 ZIP 文件，逐条校验 ZipEntry 路径防止 Zip Slip 攻击
     */
    private void extractZipSafely(Path zipFile, Path targetDir) throws IOException {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // Zip Slip 核心校验
                if (!entryPath.startsWith(targetDir)) {
                    log.warn("检测到 Zip Slip 攻击路径: {}", entry.getName());
                    throw new BusinessException("压缩包内包含非法路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    FileUtil.mkdir(entryPath.toFile());
                } else {
                    // 确保父目录存在
                    FileUtil.mkdir(entryPath.getParent().toFile());
                    // 写入文件
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }
    }

    /**
     * 校验解压后的所有文件路径均在工作空间内（二次验证）
     */
    private void validateExtractedFiles(Path workspaceDir) {
        try (Stream<Path> walk = Files.walk(workspaceDir)) {
            walk.forEach(path -> {
                Path normalized = path.normalize();
                if (!normalized.startsWith(workspaceDir)) {
                    log.warn("检测到 Zip Slip 攻击路径: {}", path);
                    throw new BusinessException("压缩包内包含非法路径");
                }
            });
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("校验解压文件失败: " + e.getMessage());
        }
    }

    /**
     * 递归列出工作空间内所有文件的相对路径
     */
    private List<String> listRelativePaths(Path workspaceDir) {
        if (!Files.exists(workspaceDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> walk = Files.walk(workspaceDir)) {
            return walk
                    .filter(path -> !Files.isDirectory(path))
                    .map(path -> workspaceDir.relativize(path).toString().replace('\\', '/'))
                    .toList();
        } catch (IOException e) {
            log.error("列出工作空间文件失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建文件树节点（递归构建子目录）
     */
    private WorkspaceFileNode buildFileNode(Path path, Path workspaceDir) {
        WorkspaceFileNode node = new WorkspaceFileNode();
        String fileName = path.getFileName().toString();
        String relativePath = workspaceDir.relativize(path).toString().replace('\\', '/');

        node.setName(fileName);
        node.setPath(relativePath);
        node.setDirectory(Files.isDirectory(path));

        if (Files.isDirectory(path)) {
            // 递归构建子节点
            try (Stream<Path> children = Files.list(path)) {
                List<WorkspaceFileNode> childNodes = children
                        .map(child -> buildFileNode(child, workspaceDir))
                        .sorted((a, b) -> {
                            if (a.isDirectory() != b.isDirectory()) {
                                return a.isDirectory() ? -1 : 1;
                            }
                            return a.getName().compareToIgnoreCase(b.getName());
                        })
                        .toList();
                node.setChildren(childNodes);
            } catch (IOException e) {
                log.warn("读取子目录失败: {}", path, e);
                node.setChildren(Collections.emptyList());
            }
        } else {
            node.setFullName(fileName);
            node.setExtension(extractExtension(fileName));
            try {
                node.setReadableSize(formatReadableSize(Files.size(path)));
            } catch (IOException e) {
                node.setReadableSize("未知");
            }
        }

        return node;
    }

    /**
     * 提取文件后缀（不含点号）
     */
    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 将字节数格式化为可读大小（如 1.5 MB）
     */
    private String formatReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }
}
