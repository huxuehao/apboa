package com.hxh.apboa.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能包导入结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillImportResult {
    /**
     * 成功导入（含覆盖）的数量
     */
    private int importedCount;

    /**
     * 因同名且未开启覆盖而跳过的数量
     */
    private int skippedCount;

    /**
     * 压缩包/目录中识别到的技能总数
     */
    private int totalCount;

    /**
     * 未识别到技能时的诊断说明
     */
    private String hintMessage;

    public SkillImportResult(int importedCount, int skippedCount, int totalCount) {
        this(importedCount, skippedCount, totalCount, null);
    }

    public static SkillImportResult withHint(int importedCount, int skippedCount, int totalCount, String hintMessage) {
        return new SkillImportResult(importedCount, skippedCount, totalCount, hintMessage);
    }
}
