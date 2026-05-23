package com.hxh.apboa.skill.controller;

import cn.hutool.core.io.FileUtil;
import com.hxh.apboa.common.exception.BusinessException;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.dto.SkillPackageDTO;
import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.enums.SkillFileType;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.mp.support.PageParams;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.util.ZipExtractUtils;
import com.hxh.apboa.common.vo.SkillImportResult;
import com.hxh.apboa.common.vo.SkillPackageVO;
import com.hxh.apboa.skill.SkillFileSystemService;
import com.hxh.apboa.skill.imports.SkillImportPathResolver;
import com.hxh.apboa.skill.imports.SkillImportService;
import com.hxh.apboa.skill.imports.SkillInstaller;
import com.hxh.apboa.skill.imports.config.GitImportConfig;
import com.hxh.apboa.skill.imports.config.LocalImportConfig;
import com.hxh.apboa.skill.imports.config.UploadImportConfig;
import com.hxh.apboa.skill.service.SkillFileService;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.hxh.apboa.skill.service.SkillToolService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 技能包Controller
 *
 * @author huxuehao
 */
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillPackageController {

    private final SkillImportService skillImportService;
    private final SkillPackageService skillPackageService;
    private final SkillToolService skillToolService;
    private final SkillFileService skillFileService;

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<SkillPackageVO>> page(PageParams pageParams, SkillPackageDTO query) {
        IPage<SkillPackage> page = skillPackageService.page(MP.getPage(pageParams), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, SkillPackageVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<SkillPackageVO> detail(@PathVariable("id") Long id) {
        SkillPackageVO vo = skillPackageService.getDetail(id);
        if (vo != null) {
            vo.setUsed(skillPackageService.usedWithAgent(List.of(id)));
        }
        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Long> save(@RequestBody SkillPackageVO vo) {
        SkillPackage entity = BeanUtils.copy(vo, SkillPackage.class);
        skillPackageService.save(entity);
        // 保存技能与工具的关联
        if (vo.getTools() != null && !vo.getTools().isEmpty()) {
            skillToolService.saveSkillTool(entity.getId(), vo.getTools());
        }
        // 创建技能包目录并写入默认 SKILL.md（带 YAML 头）
        SkillFileSystemService.buildSkillDir(entity.getName());
        String initialContent = SkillFileSystemService.buildSkillMdContent(entity.getName(), entity.getDescription());
        SkillFileSystemService.writeFile(entity.getName(), "SKILL.md", initialContent);
        // 写入 SKILL.md 文件记录
        SkillFile skillFile = new SkillFile();
        skillFile.setSkillId(entity.getId());
        skillFile.setFileType(SkillFileType.SKILL_MD);
        skillFile.setFileName("SKILL.md");
        skillFile.setFilePath("SKILL.md");
        skillFile.setContent(initialContent);
        skillFile.setSort(0);
        skillFileService.save(skillFile);
        return R.data(entity.getId());
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody SkillPackageVO vo) {
        SkillPackage entity = BeanUtils.copy(vo, SkillPackage.class);
        boolean b = skillPackageService.doUpdate(entity);
        // 更新技能与工具的关联
        skillToolService.saveSkillTool(entity.getId(), vo.getTools());
        return R.data(b);
    }

    /**
     * 删除
     */
    @DeleteMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> delete(@RequestBody List<Long> ids) {
        List<SkillPackage> skillPackages = skillPackageService.listByIds(ids);
        for (SkillPackage skillPackage : skillPackages) {
            // 卸载技能包目录
            SkillInstaller.uninstall(skillPackage.getName());
            // 删除文件系统目录
            SkillFileSystemService.removeSkillDir(skillPackage.getName());
            // 删除 DB 文件记录
            skillFileService.deleteBySkillId(skillPackage.getId());
        }
        return R.data(skillPackageService.deleteByIds(ids));
    }

    /**
     * 被哪些Agent使用
     */
    @PostMapping("used-with-agent")
    public R<List<Object>> usedWithAgent(@RequestBody List<Long> ids) {
        return R.data(skillPackageService.usedWithAgent(ids));
    }

    /**
     * 获取所有分类
     */
    @GetMapping("/get/categories")
    public R<List<String>> listCategories() {
        return R.data(skillPackageService.listCategories());
    }

    /**
     * 从git导入
     */
    @PostMapping("/import/git")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<SkillImportResult> importFromGit(@RequestBody GitImportConfig config) {
        return R.data(skillImportService.importFromGit(config));
    }

    /**
     * 从本地导入
     */
    @PostMapping("/import/local")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<SkillImportResult> importFromLocal(@RequestBody LocalImportConfig config) {
        return R.data(skillImportService.importFromLocal(config));
    }
    /**
     * 从压缩包导入
     *
     * @param file     技能包压缩包（zip 格式，解压后需包含 skills/ 目录）
     * @param category 技能分类
     * @param cover    是否覆盖已存在的同名技能
     */
    @PostMapping("/import/upload")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<SkillImportResult> importFromUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam("cover") boolean cover) throws IOException {

        // 确保 .apboa/temp 目录存在
        Path tempBase = Paths.get(SysConst.ROOT_DIR_NAME, "temp");
        Files.createDirectories(tempBase);

        // 生成唯一 UUID 作为本次解压目录名
        String uuid = UUID.randomUUID().toString();
        Path extractDir = tempBase.resolve(uuid);
        Files.createDirectories(extractDir);

        Path tempZip = tempBase.resolve(uuid + ".zip");
        try {
            ZipExtractUtils.extractZipSafely(file.getInputStream(), extractDir, tempZip);
        } catch (IOException e) {
            FileUtil.del(extractDir.toFile());
            throw new BusinessException("压缩包解压失败，请确认文件为有效 zip 格式: " + e.getMessage());
        }

        // 解析 skills 根目录（兼容压缩包多套一层目录的结构）
        Path skillsDir;
        try {
            skillsDir = SkillImportPathResolver.resolveUploadedSkillsDir(extractDir);
        } catch (RuntimeException e) {
            FileUtil.del(extractDir.toFile());
            throw e;
        }

        UploadImportConfig config = UploadImportConfig.builder()
                .category(category)
                .cover(cover)
                .templatePath(skillsDir.toString())
                .extractDirPath(extractDir.toString())
                .build();

        return R.data(skillImportService.importFromUpload(config));
    }
}
