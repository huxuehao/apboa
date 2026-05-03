package com.hxh.apboa.skill.imports;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.util.FolderUtils;
import com.hxh.apboa.skill.imports.config.GitImportConfig;
import com.hxh.apboa.skill.imports.config.LocalImportConfig;
import com.hxh.apboa.skill.imports.config.UploadImportConfig;
import com.hxh.apboa.skill.service.SkillPackageService;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.skill.repository.GitSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

    /**
     * 从 Git 导入
     * 使用临时目录克隆仓库，导入完成后清理临时目录
     *
     * @param config 配置
     */
    public boolean importFromGit(GitImportConfig config) {
        Path tempDir = createTempDir();
        try {
            GitSkillRepository repo = new GitSkillRepository(config.getRepoUrl(), tempDir);
            try {
                Path skillsDir = resolveSkillsDir(tempDir);
                doImport(skillsDir, repo, config.isCover(), config.getCategory());
            } finally {
                closeQuietly(repo);
            }
        } finally {
            FolderUtils.deleteRecursively(tempDir.toAbsolutePath().toString());
            log.info("清理 Git 临时目录: {}", tempDir.toAbsolutePath());
        }
        return true;
    }

    /**
     * 从本地导入
     * 当本地路径与 SKILLS_DIR 相同时，跳过文件复制，仅更新 DB
     *
     * @param config 配置
     */
    public boolean importFromLocal(LocalImportConfig config) {
        Path skillsDir = Path.of(config.getPath());
        try (AgentSkillRepository repo = new FileSystemSkillRepository(skillsDir)) {
            doImport(skillsDir, repo, config.isCover(), config.getCategory());
        }
        return true;
    }

    /**
     * 从上传压缩包导入
     * 调用该方法前，需要将上传的压缩包解压到临时目录，完成后会自动删除临时目录
     *
     * @param config 配置
     */
    public boolean importFromUpload(UploadImportConfig config) {
        Path skillsPath = Path.of(config.getTemplatePath());
        try (AgentSkillRepository repo = new FileSystemSkillRepository(skillsPath)) {
            doImport(skillsPath, repo, config.isCover(), config.getCategory());
        } finally {
            // 基于 templatePath 向上一层定位 UUID 临时目录并删除
            Path tempUuidDir = skillsPath.getParent();
            if (tempUuidDir != null) {
                FolderUtils.deleteRecursively(tempUuidDir.toAbsolutePath().toString());
                log.info("清理上传临时目录: {}", tempUuidDir.toAbsolutePath());
            }
        }
        return true;
    }

    /**
     * 执行导入
     *
     * @param skillsDir 技能包根目录（包含各技能包子目录）
     * @param repo      AgentSkillRepository
     * @param isCover   是否覆盖
     * @param category  分类
     */
    private void doImport(Path skillsDir, AgentSkillRepository repo, boolean isCover, String category) {
        List<String> allSkillNames = repo.getAllSkillNames();

        for (String skillName : allSkillNames) {
            Path sourceSkillDir = skillsDir.resolve(skillName);

            // 安装技能包目录到 SKILLS_DIR（含覆盖策略）
            boolean installed = SkillInstaller.install(sourceSkillDir, skillName, isCover);
            if (!installed) {
                log.info("技能包 {} 已存在且跳过覆盖", skillName);
                continue;
            }

            // DB 持久化
            AgentSkill agentSkill = repo.getSkill(skillName);
            SkillPackage skillPackage = SkillPackageBuilder.build(agentSkill, category);

            SkillPackage oldSkillPackage = skillPackageService.getOne(
                    new LambdaQueryWrapper<SkillPackage>()
                            .eq(SkillPackage::getName, skillName),
                    false);

            if (oldSkillPackage == null) {
                skillPackageService.save(skillPackage);
            } else {
                skillPackage.setId(oldSkillPackage.getId());
                skillPackageService.updateById(skillPackage);
            }
        }
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
     * 解析技能包根目录
     * 优先使用 skills/ 子目录，否则使用根目录
     *
     * @param baseDir 基础目录
     * @return 技能包根目录
     */
    private Path resolveSkillsDir(Path baseDir) {
        Path skillsSubDir = baseDir.resolve(SysConst.SKILLS_DIR_NAME);
        if (Files.isDirectory(skillsSubDir)) {
            return skillsSubDir;
        }
        return baseDir;
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
