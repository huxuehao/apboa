package com.hxh.apboa.skill.imports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.util.FolderUtils;
import com.hxh.apboa.common.vo.SkillImportResult;
import com.hxh.apboa.skill.SkillFileSystemService;
import com.hxh.apboa.skill.imports.config.GitImportConfig;
import com.hxh.apboa.skill.imports.config.LocalImportConfig;
import com.hxh.apboa.skill.imports.config.UploadImportConfig;
import com.hxh.apboa.skill.service.SkillFileService;
import com.hxh.apboa.skill.service.SkillPackageService;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.skill.repository.GitSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 描述：技能包导入服务，编排本地/压缩包/Git三种导入方式
 *
 * @author huxuehao
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillImportService {
    private final SkillPackageService skillPackageService;
    private final SkillFileService skillFileService;

    /**
     * 从 Git 导入
     * 使用临时目录克隆仓库，导入完成后清理临时目录
     *
     * @param config 配置
     */
    public SkillImportResult importFromGit(GitImportConfig config) {
        Path tempDir = createTempDir();
        try {
            GitSkillRepository repo = new GitSkillRepository(config.getRepoUrl(), tempDir);
            try {
                Path skillsDir = SkillImportPathResolver.resolveSkillsDir(tempDir);
                return doImport(skillsDir, repo, config.isCover(), config.getCategory());
            } finally {
                closeQuietly(repo);
            }
        } finally {
            FolderUtils.deleteRecursively(tempDir.toAbsolutePath().toString());
            log.info("清理 Git 临时目录: {}", tempDir.toAbsolutePath());
        }
    }

    /**
     * 从本地导入
     * 当本地路径与 SKILLS_DIR 相同时，跳过文件复制，仅更新 DB
     *
     * @param config 配置
     */
    public SkillImportResult importFromLocal(LocalImportConfig config) {
        Path skillsDir = Path.of(config.getPath());
        try (AgentSkillRepository repo = new FileSystemSkillRepository(skillsDir)) {
            return doImport(skillsDir, repo, config.isCover(), config.getCategory());
        }
    }

    /**
     * 从上传压缩包导入
     * 调用该方法前，需要将上传的压缩包解压到临时目录，完成后会自动删除临时目录
     *
     * @param config 配置
     */
    public SkillImportResult importFromUpload(UploadImportConfig config) {
        Path skillsPath = Path.of(config.getTemplatePath());
        Path tempRoot = config.getExtractDirPath() != null
                ? Path.of(config.getExtractDirPath())
                : skillsPath.getParent();
        try (AgentSkillRepository repo = new FileSystemSkillRepository(skillsPath)) {
            return doImport(skillsPath, repo, config.isCover(), config.getCategory());
        } finally {
            if (tempRoot != null) {
                FolderUtils.deleteRecursively(tempRoot.toAbsolutePath().toString());
                log.info("清理上传临时目录: {}", tempRoot.toAbsolutePath());
            }
        }
    }

    /**
     * 执行导入
     *
     * @param skillsDir 技能包根目录（包含各技能包子目录）
     * @param repo      AgentSkillRepository
     * @param isCover   是否覆盖
     * @param category  分类
     */
    private SkillImportResult doImport(Path skillsDir, AgentSkillRepository repo, boolean isCover, String category) {
        try {
            SkillImportNormalizer.normalizeSkillFiles(skillsDir);
        } catch (IOException e) {
            log.warn("Normalize skill files failed: {}", e.getMessage());
        }

        List<String> allSkillNames = repo.getAllSkillNames();
        if (allSkillNames.isEmpty()) {
            return SkillImportResult.withHint(0, 0, 0, SkillImportInspector.buildHint(skillsDir));
        }

        int importedCount = 0;
        int skippedCount = 0;

        for (String skillName : allSkillNames) {
            Optional<Path> sourceSkillDir = SkillImportInspector.findSkillDirectory(skillsDir, skillName);
            if (sourceSkillDir.isEmpty()) {
                log.warn("技能 {} 源目录未找到，跳过安装", skillName);
                skippedCount++;
                continue;
            }

            boolean installed = SkillInstaller.install(sourceSkillDir.get(), skillName, isCover);
            if (!installed) {
                log.info("技能包 {} 已存在且跳过覆盖", skillName);
                skippedCount++;
                continue;
            }

            AgentSkill agentSkill = repo.getSkill(skillName);
            SkillPackageBuilder.BuildResult buildResult = SkillPackageBuilder.build(agentSkill, category);
            SkillPackage skillPackage = buildResult.getSkillPackage();
            List<SkillFile> skillFiles = buildResult.getSkillFiles();

            SkillPackage oldSkillPackage = skillPackageService.getOne(
                    new LambdaQueryWrapper<SkillPackage>()
                            .eq(SkillPackage::getName, skillName),
                    false);

            Long skillId;
            if (oldSkillPackage == null) {
                skillPackageService.save(skillPackage);
                skillId = skillPackage.getId();
            } else {
                skillPackage.setId(oldSkillPackage.getId());
                skillPackage.setEnabled(oldSkillPackage.getEnabled() != null ? oldSkillPackage.getEnabled() : Boolean.TRUE);
                skillPackageService.updateById(skillPackage);
                skillId = oldSkillPackage.getId();
                // 删除旧的文件记录
                skillFileService.deleteBySkillId(skillId);
            }

            // 保存技能文件到 DB 并同步到文件系统
            for (SkillFile sf : skillFiles) {
                sf.setSkillId(skillId);
                skillFileService.save(sf);
                // 同步到文件系统
                SkillFileSystemService.writeFile(skillPackage.getName(), sf.getFilePath(), sf.getContent());
            }

            importedCount++;
        }

        return new SkillImportResult(importedCount, skippedCount, allSkillNames.size());
    }

    /**
     * 创建临时目录（.apboa/temp/{uuid}/）
     *
     * @return 临时目录路径
     */
    private Path createTempDir() {
        Path tempBase = Paths.get(SysConst.ROOT_DIR_NAME, "temp");
        FolderUtils.mkdirsByAbsolutePath(tempBase.toAbsolutePath().toString());
        Path tempDir = tempBase.resolve(UUID.randomUUID().toString());
        FolderUtils.mkdirsByAbsolutePath(tempDir.toAbsolutePath().toString());
        return tempDir;
    }

    /**
     * 安静关闭 GitSkillRepository
     *
     * @param repo GitSkillRepository 实例
     */
    private void closeQuietly(GitSkillRepository repo) {
        if (repo != null) {
            try {
                repo.close();
            } catch (Exception e) {
                // Windows 下文件占用是正常现象，只打印警告
                log.warn("关闭 Git 仓库临时目录时出现文件占用（Windows 环境可忽略）：{}", e.getMessage());
            }
        }
    }
}
