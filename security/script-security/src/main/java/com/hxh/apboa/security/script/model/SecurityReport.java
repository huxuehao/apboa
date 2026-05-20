package com.hxh.apboa.security.script.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 描述：脚本安全检查的完整报告
 *
 * @param fileName              被检查的文件名
 * @param scriptType            识别的脚本类型
 * @param safe                  是否安全（无 HIGH 或 CRITICAL 级别发现即为安全）
 * @param findings              所有发现的不安全行为列表
 * @param totalCount            发现总数
 * @param severityDistribution  各严重等级的发现数量分布
 * @param categoryDistribution  各分类的发现数量分布
 * @author huxuehao
 */
public record SecurityReport(
        String fileName,
        ScriptType scriptType,
        boolean safe,
        List<SecurityFinding> findings,
        int totalCount,
        Map<Severity, Long> severityDistribution,
        Map<FindingCategory, Long> categoryDistribution
) {

    /**
     * 根据检查结果生成报告
     *
     * @param fileName   文件名
     * @param scriptType 脚本类型
     * @param findings   发现列表
     * @return 完整的安全检查报告
     */
    public static SecurityReport of(String fileName, ScriptType scriptType, List<SecurityFinding> findings) {
        // 统计各严重等级分布
        Map<Severity, Long> severityDist = findings.stream()
                .collect(Collectors.groupingBy(
                        SecurityFinding::severity,
                        () -> new EnumMap<>(Severity.class),
                        Collectors.counting()
                ));

        // 统计各分类分布
        Map<FindingCategory, Long> categoryDist = findings.stream()
                .collect(Collectors.groupingBy(
                        SecurityFinding::category,
                        () -> new EnumMap<>(FindingCategory.class),
                        Collectors.counting()
                ));

        // 无 HIGH 或 CRITICAL 级别发现即为安全
        boolean isSafe = findings.stream()
                .noneMatch(f -> f.severity() == Severity.HIGH || f.severity() == Severity.CRITICAL);

        return new SecurityReport(
                fileName,
                scriptType,
                isSafe,
                Collections.unmodifiableList(findings),
                findings.size(),
                Collections.unmodifiableMap(severityDist),
                Collections.unmodifiableMap(categoryDist)
        );
    }

}
