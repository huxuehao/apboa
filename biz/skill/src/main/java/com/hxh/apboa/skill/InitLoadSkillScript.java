package com.hxh.apboa.skill;

import com.hxh.apboa.common.entity.SkillFile;
import com.hxh.apboa.common.entity.SkillPackage;
import com.hxh.apboa.skill.service.SkillFileService;
import com.hxh.apboa.skill.service.SkillPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 描述：启动时同步技能包文件到文件系统，并补录 DB 中缺失的记录
 *
 * @author huxuehao
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class InitLoadSkillScript implements ApplicationRunner {
    private final SkillPackageService skillPackageService;
    private final SkillFileService skillFileService;

    @Override
    public void run(ApplicationArguments args) {
        List<SkillPackage> list = skillPackageService.list();
        for (SkillPackage skillPackage : list) {
            // 将 DB 中的入库文件同步到文件系统
            List<SkillFile> files = skillFileService.listBySkillId(skillPackage.getId());
            for (SkillFile file : files) {
                SkillFileSystemService.writeFile(skillPackage.getName(), file.getFilePath(), file.getContent());
            }

            // 扫描文件系统，补录 DB 中缺失的入库文件
            List<SkillFileSystemService.FileTreeNode> fsNodes =
                    SkillFileSystemService.scanSkillTree(skillPackage.getName());
            syncMissingFiles(skillPackage, fsNodes);

            log.info("已同步技能包 {} 的文件到本地", skillPackage.getName());
        }
    }

    /**
     * 递归扫描文件系统，将符合入库规则但 DB 中不存在的文件补录到 DB
     */
    private void syncMissingFiles(SkillPackage skillPackage, List<SkillFileSystemService.FileTreeNode> nodes) {
        for (SkillFileSystemService.FileTreeNode node : nodes) {
            if (node.isDirectory()) {
                syncMissingFiles(skillPackage, node.getChildren());
                continue;
            }

            String relPath = node.getPath().replace('\\', '/');
            if (!SkillFileSystemService.shouldPersistToDb(relPath)) {
                continue;
            }

            // 检查 DB 中是否已存在
            SkillFile existing = skillFileService.getBySkillIdAndPath(skillPackage.getId(), relPath);
            if (existing != null) {
                continue;
            }

            // 读取文件内容并写入 DB
            String content = SkillFileSystemService.readFileContent(skillPackage.getName(), relPath);
            SkillFile sf = new SkillFile();
            sf.setSkillId(skillPackage.getId());
            sf.setFileType(SkillFileSystemService.resolveFileType(relPath));
            sf.setFileName(node.getName());
            sf.setFilePath(relPath);
            sf.setContent(content != null ? content : "");
            sf.setSort(0);
            skillFileService.save(sf);

            log.info("补录技能包文件到 DB: skillName={}, path={}", skillPackage.getName(), relPath);
        }
    }
}
