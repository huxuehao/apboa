package com.hxh.apboa.skill.controller;

import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.enums.SkillFileType;
import com.hxh.apboa.common.exception.BusinessException;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.vo.SkillFileTreeNodeVO;
import com.hxh.apboa.skill.SkillFileSystemService;
import com.hxh.apboa.skill.SkillFileSystemService.FileTreeNode;
import com.hxh.apboa.skill.service.SkillFileService;
import com.hxh.apboa.skill.service.SkillPackageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 描述：技能包文件Controller
 *
 * @author huxuehao
 **/
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillFileController {

    private final SkillFileService skillFileService;
    private final SkillPackageService skillPackageService;

    /**
     * 获取技能包文件树
     */
    @GetMapping("/{skillId}/tree")
    public R<List<SkillFileTreeNodeVO>> getTree(@PathVariable("skillId") Long skillId) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        // 1. 扫描文件系统
        List<FileTreeNode> fsNodes = SkillFileSystemService.scanSkillTree(skillPackage.getName());

        // 2. 查询 DB 中入库的文件
        List<SkillFile> dbFiles = skillFileService.listBySkillId(skillId);
        Map<String, SkillFile> dbFileMap = new HashMap<>();
        for (SkillFile sf : dbFiles) {
            String normalizedPath = sf.getFilePath().replace('\\', '/');
            dbFileMap.put(normalizedPath, sf);
        }

        // 3. 合并：为入库文件挂载 DB 信息
        List<SkillFileTreeNodeVO> result = convertToVo(fsNodes, dbFileMap);

        return R.data(result);
    }

    /**
     * 创建文件
     */
    @PostMapping("/{skillId}/files")
    public R<SkillFileTreeNodeVO> createFile(@PathVariable("skillId") Long skillId,
                                              @RequestBody CreateFileRequest request) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        // 构建完整路径
        String parentPath = request.getParentPath();
        if (parentPath != null && !parentPath.isEmpty() && !parentPath.endsWith("/")) {
            parentPath += "/";
        }
        String relativePath = (parentPath != null ? parentPath : "") + request.getFileName();

        // 写入文件内容
        String content = request.getContent() != null ? request.getContent() : "";
        SkillFileSystemService.writeFile(skillPackage.getName(), relativePath, content);

        // 判断是否需要入库
        SkillFileTreeNodeVO vo = new SkillFileTreeNodeVO();
        if (SkillFileSystemService.shouldPersistToDb(relativePath)) {
            SkillFileType fileType = SkillFileSystemService.resolveFileType(relativePath);

            SkillFile file = new SkillFile();
            file.setSkillId(skillId);
            file.setFileType(fileType);
            file.setFileName(request.getFileName());
            file.setFilePath(relativePath);
            file.setContent(content);
            file.setSort(0);
            skillFileService.save(file);

            vo.setFileId(file.getId());
            vo.setFileType(fileType.name());
        }

        vo.setName(request.getFileName());
        vo.setPath(relativePath);
        vo.setDirectory(false);
        vo.setExtension(getExtension(request.getFileName()));

        return R.data(vo);
    }

    /**
     * 更新入库文件内容
     */
    @PutMapping("/files/{fileId}")
    public R<Boolean> updateFile(@PathVariable("fileId") Long fileId, @RequestBody UpdateFileRequest request) {
        SkillFile file = skillFileService.getById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        SkillPackage skillPackage = skillPackageService.getById(file.getSkillId());
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        // 更新 DB
        skillFileService.updateContent(fileId, request.getContent());

        // 同步到文件系统
        SkillFileSystemService.writeFile(skillPackage.getName(), file.getFilePath(), request.getContent());

        // 如果是 SKILL.md，解析 YAML 头并同步 name / description 到 skill_package
        if (file.getFileType() == SkillFileType.SKILL_MD) {
            Map<String, String> header = SkillFileSystemService.parseSkillMdHeader(request.getContent());
            String newName = header.get("name");
            String newDescription = header.get("description");
            boolean needUpdate = false;
            if (newName != null && !newName.isEmpty() && !newName.equals(skillPackage.getName())) {
                skillPackage.setName(newName);
                needUpdate = true;
            }
            if (newDescription != null && !newDescription.equals(skillPackage.getDescription())) {
                skillPackage.setDescription(newDescription);
                needUpdate = true;
            }
            if (needUpdate) {
                skillPackageService.updateById(skillPackage);
            }
        }

        return R.data(true);
    }

    /**
     * 更新纯文件系统文件
     */
    @PutMapping("/{skillId}/filesystem-write")
    public R<Boolean> writeFileSystemFile(@PathVariable("skillId") Long skillId,
                                           @RequestBody WriteFileSystemRequest request) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        SkillFileSystemService.writeFile(skillPackage.getName(), request.getPath(), request.getContent());
        return R.data(true);
    }

    /**
     * 删除入库文件
     */
    @DeleteMapping("/files/{fileId}")
    public R<Boolean> deleteDbFile(@PathVariable("fileId") Long fileId) {
        SkillFile file = skillFileService.getById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        String fileName = file.getFileName();
        // SKILL.md 不允许删除
        if ("SKILL.md".equalsIgnoreCase(fileName)) {
            throw new BusinessException("SKILL.md 不允许删除");
        }

        SkillPackage skillPackage = skillPackageService.getById(file.getSkillId());
        if (skillPackage != null) {
            SkillFileSystemService.deleteFile(skillPackage.getName(), file.getFilePath());
        }

        skillFileService.removeById(fileId);
        return R.data(true);
    }

    /**
     * 删除纯文件系统节点（文件或目录）
     * 同时清理 skill_file 表中的相关记录，避免孤儿数据
     */
    @DeleteMapping("/{skillId}/filesystem")
    public R<Boolean> deleteFileSystemNode(@PathVariable("skillId") Long skillId,
                                            @RequestBody DeleteFileSystemRequest request) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        if (request.isDirectory()) {
            // 先清理该目录下所有入库文件的 DB 记录
            String pathPrefix = request.getPath();
            if (!pathPrefix.endsWith("/")) {
                pathPrefix += "/";
            }
            skillFileService.removeBySkillIdAndPathPrefix(skillId, pathPrefix);
            // 再递归删除文件系统目录
            SkillFileSystemService.deleteDirectory(skillPackage.getName(), request.getPath());
        } else {
            // 清理可能存在的孤儿 DB 记录
            skillFileService.removeBySkillIdAndPath(skillId, request.getPath());
            SkillFileSystemService.deleteFile(skillPackage.getName(), request.getPath());
        }
        return R.data(true);
    }

    /**
     * 创建文件夹
     */
    @PostMapping("/{skillId}/directories")
    public R<Boolean> createDirectory(@PathVariable("skillId") Long skillId,
                                       @RequestBody CreateDirectoryRequest request) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        String parentPath = request.getParentPath();
        if (parentPath != null && !parentPath.isEmpty() && !parentPath.endsWith("/")) {
            parentPath += "/";
        }
        String relativePath = (parentPath != null ? parentPath : "") + request.getDirName();

        SkillFileSystemService.createDirectory(skillPackage.getName(), relativePath);
        return R.data(true);
    }

    /**
     * 上传文件
     */
    @PostMapping("/{skillId}/upload")
    public R<SkillFileTreeNodeVO> uploadFile(@PathVariable("skillId") Long skillId,
                                              @RequestParam("parentPath") String parentPath,
                                              @RequestParam("file") MultipartFile file) throws Exception {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 构建路径
        String normalizedParent = parentPath != null ? parentPath : "";
        if (!normalizedParent.isEmpty() && !normalizedParent.endsWith("/")) {
            normalizedParent += "/";
        }
        String relativePath = normalizedParent + fileName;

        // 判断是否需要入库（白名单文本文件才入库，二进制文件仅写入文件系统）
        boolean shouldPersist = SkillFileSystemService.shouldPersistToDb(relativePath);
        SkillFileTreeNodeVO vo = new SkillFileTreeNodeVO();

        if (shouldPersist) {
            // 文本文件：String 方式写入并入库
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            SkillFileSystemService.writeFile(skillPackage.getName(), relativePath, content);

            SkillFileType fileType = SkillFileSystemService.resolveFileType(relativePath);
            SkillFile sf = new SkillFile();
            sf.setSkillId(skillId);
            sf.setFileType(fileType);
            sf.setFileName(fileName);
            sf.setFilePath(relativePath);
            sf.setContent(content);
            sf.setSort(0);
            skillFileService.save(sf);

            vo.setFileId(sf.getId());
            vo.setFileType(fileType.name());
        } else {
            // 二进制文件：直接写原始字节，不经过 String 转换
            SkillFileSystemService.writeFileBytes(skillPackage.getName(), relativePath, file.getBytes());
        }

        vo.setName(fileName);
        vo.setPath(relativePath);
        vo.setDirectory(false);
        vo.setExtension(getExtension(fileName));

        return R.data(vo);
    }

    /**
     * 读取纯文件系统文件内容
     */
    @GetMapping("/{skillId}/file-content")
    public R<String> getFileContent(@PathVariable("skillId") Long skillId, @RequestParam("path") String path) {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        String content = SkillFileSystemService.readFileContent(skillPackage.getName(), path);
        return R.data(content);
    }

    /**
     * 获取允许的文件扩展名白名单
     */
    @GetMapping("/allowed-extensions")
    public R<List<String>> getAllowedExtensions() {
        return R.data(SkillFileSystemService.getAllowedExtensions());
    }

    /**
     * 按路径从文件系统下载文件
     */
    @GetMapping("/{skillId}/download")
    public void downloadFile(@PathVariable("skillId") Long skillId,
                              @RequestParam("path") String path,
                              HttpServletResponse response) throws IOException {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        Path skillDir = Paths.get(SysConst.SKILLS_DIR, skillPackage.getName());
        Path filePath = skillDir.resolve(path).normalize();
        if (!filePath.startsWith(skillDir)) {
            throw new BusinessException("非法文件路径");
        }
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new BusinessException("文件不存在");
        }

        String fileName = filePath.getFileName().toString();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
            new String(fileName.getBytes(StandardCharsets.UTF_8), "ISO-8859-1") + "\"");
        response.setContentLengthLong(Files.size(filePath));

        try (OutputStream out = response.getOutputStream();
             InputStream in = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    /**
     * 下载整个技能包为压缩包
     */
    @GetMapping("/{skillId}/download-zip")
    public void downloadZip(@PathVariable("skillId") Long skillId,
                             HttpServletResponse response) throws IOException {
        SkillPackage skillPackage = skillPackageService.getById(skillId);
        if (skillPackage == null) {
            throw new BusinessException("技能包不存在");
        }

        Path skillDir = Paths.get(SysConst.SKILLS_DIR, skillPackage.getName());
        if (!Files.exists(skillDir)) {
            throw new BusinessException("技能包目录不存在");
        }

        String zipFileName = skillPackage.getName() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
            new String(zipFileName.getBytes(StandardCharsets.UTF_8), "ISO-8859-1") + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            Files.walk(skillDir).forEach(filePath -> {
                if (Files.isDirectory(filePath)) return;
                String entryName = skillDir.relativize(filePath).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("压缩文件失败: " + filePath, e);
                }
            });
            zos.flush();
        }
    }

    /**
     * 将文件系统树节点转换为 VO，合并 DB 信息
     */
    private List<SkillFileTreeNodeVO> convertToVo(List<FileTreeNode> fsNodes,
                                                    Map<String, SkillFile> dbFileMap) {
        if (fsNodes == null) {
            return Collections.emptyList();
        }

        List<SkillFileTreeNodeVO> result = new ArrayList<>();
        for (FileTreeNode fsNode : fsNodes) {
            SkillFileTreeNodeVO vo = new SkillFileTreeNodeVO();
            vo.setName(fsNode.getName());
            vo.setPath(fsNode.getPath());
            vo.setDirectory(fsNode.isDirectory());
            vo.setExtension(fsNode.getExtension());
            vo.setFileSize(fsNode.getFileSize());

            if (!fsNode.isDirectory()) {
                String normalizedPath = fsNode.getPath().replace('\\', '/');
                SkillFile dbFile = dbFileMap.get(normalizedPath);
                if (dbFile != null) {
                    vo.setFileId(dbFile.getId());
                    vo.setFileType(dbFile.getFileType().name());
                }
            }

            if (!fsNode.getChildren().isEmpty()) {
                vo.setChildren(convertToVo(fsNode.getChildren(), dbFileMap));
            }

            result.add(vo);
        }

        return result;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    // ==================== 请求 DTO ====================

    @Data
    public static class CreateFileRequest {
        private String parentPath;
        private String fileName;
        private String content;
    }

    @Data
    public static class UpdateFileRequest {
        private String content;
    }

    @Data
    public static class WriteFileSystemRequest {
        private String path;
        private String content;
    }

    @Data
    public static class DeleteFileSystemRequest {
        private String path;
        private boolean directory;
    }

    @Data
    public static class CreateDirectoryRequest {
        private String parentPath;
        private String dirName;
    }
}
