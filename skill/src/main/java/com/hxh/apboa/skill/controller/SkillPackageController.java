package com.hxh.apboa.skill.controller;

import cn.hutool.core.io.FileUtil;
import com.hxh.apboa.common.config.auth.RoleNeed;
import com.hxh.apboa.common.dto.SkillPackageDTO;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.common.enums.Role;
import com.hxh.apboa.common.mp.support.MP;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.common.util.BeanUtils;
import com.hxh.apboa.common.vo.SkillPackageVO;
import com.hxh.apboa.skill.SkillScriptLoadHelper;
import com.hxh.apboa.skill.imports.SkillImportService;
import com.hxh.apboa.skill.imports.config.GitImportConfig;
import com.hxh.apboa.skill.imports.config.LocalImportConfig;
import com.hxh.apboa.skill.imports.config.UploadImportConfig;
import com.hxh.apboa.skill.service.SkillPackageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public R<IPage<SkillPackageVO>> page(SkillPackageDTO query) {
        IPage<SkillPackage> page = skillPackageService.page(MP.<SkillPackage>getPage(query), MP.getQueryWrapper(query));
        return R.data(BeanUtils.copyPage(page, SkillPackageVO.class));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public R<SkillPackageVO> detail(@PathVariable("id") Long id) {
        SkillPackage entity = skillPackageService.getById(id);

        SkillPackageVO vo = BeanUtils.copy(entity, SkillPackageVO.class);
        vo.setUsed(skillPackageService.usedWithAgent(List.of(id)));

        return R.data(vo);
    }

    /**
     * 新增
     */
    @PostMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> save(@RequestBody SkillPackage entity) {
        boolean save = skillPackageService.save(entity);
        // 尝试装载脚本到本地
        SkillScriptLoadHelper.loadScripts(entity);
        return R.data(save);
    }

    /**
     * 修改
     */
    @PutMapping
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> update(@RequestBody SkillPackage entity) {
        boolean b = skillPackageService.updateById(entity);
        // 尝试装载脚本到本地
        SkillScriptLoadHelper.loadScripts(entity);
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
            // 尝试删除本地装载的脚本
            SkillScriptLoadHelper.removeScripts(skillPackage);
            skillPackageService.removeById(skillPackage.getId());
        }
        return R.data(true);
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
    public R<Boolean> importFromGit(@RequestBody GitImportConfig config) {
        return R.data(skillImportService.importFromGit(config));
    }

    /**
     * 从本地导入
     */
    @PostMapping("/import/local")
    @RoleNeed({Role.ADMIN, Role.EDIT})
    public R<Boolean> importFromLocal(@RequestBody LocalImportConfig config) {
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
    public R<Boolean> importFromUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam("cover") boolean cover) throws IOException {

        // 确保 .apboa/temp 目录存在
        Path tempBase = Paths.get(".apboa", "temp");
        Files.createDirectories(tempBase);

        // 生成唯一 UUID 作为本次解压目录名
        String uuid = UUID.randomUUID().toString();
        Path extractDir = tempBase.resolve(uuid);
        Files.createDirectories(extractDir);

        // 将压缩包解压到 .apboa/temp/[UUID]/ 下
        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = extractDir.resolve(entry.getName()).normalize();
                // 防止 zip slip 攻击
                if (!entryPath.startsWith(extractDir)) {
                    throw new IOException("非法的压缩包路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            // 解压失败时清理已创建的临时目录，避免泄漏
            FileUtil.del(extractDir.toFile());
            throw e;
        }

        // 构建导入配置，templatePath 指向解压目录中的 skills/ 子目录
        UploadImportConfig config = UploadImportConfig.builder()
                .category(category)
                .cover(cover)
                .templatePath(extractDir.resolve("skills").toString())
                .build();

        return R.data(skillImportService.importFromUpload(config));
    }
}
