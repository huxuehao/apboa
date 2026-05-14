package com.hxh.apboa.core.mpatch.core;

import com.hxh.apboa.core.mpatch.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码增量更新器
 * 实现基于 SEARCH/REPLACE 语法的代码增量更新功能
 *
 * @author huxuehao
 * 支持三种操作场景：
 * 1. 新增：在 SEARCH 锚点前后拼接新代码
 * 2. 删除：将 SEARCH 片段替换为空
 * 3. 修改：用完整原代码作 SEARCH，修改后代码作 REPLACE
 */
public class CodeIncrementalUpdater {

    /**
     * 单例实例
     */
    private static final CodeIncrementalUpdater INSTANCE = new CodeIncrementalUpdater();

    /**
     * 通用代码块正则：匹配任意语言的代码块（```xxx ... ```）
     */
    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("```\\w*\\r?\\n([\\s\\S]*?)(?:\\r?\\n```|$)");

    /**
     * SEARCH/REPLACE 格式正则
     * 格式：
     * ------- SEARCH
     * (要搜索的原代码片段)
     * =======
     * (要替换成的新代码片段)
     * +++++++ REPLACE
     * <br/>
     * 不支持
     * ------- SEARCH
     * (要搜索的原代码片段)
     * =======
     * +++++++ REPLACE
     */
    private static final Pattern SEARCH_REPLACE_PATTERN =
            Pattern.compile("-{7,}\\s*SEARCH\\s*\\n([\\s\\S]*?)\\n={7,}\\s*\\n([\\s\\S]*?)\\n\\+{7,}\\s*REPLACE");

    /**
     * SEARCH/REPLACE 格式正则
     * 格式：
     * ------- SEARCH
     * (要搜索的原代码片段)
     * =======
     * (要替换成的新代码片段)
     * +++++++ REPLACE
     * <br/>
     * 支持
     * ------- SEARCH
     * (要搜索的原代码片段)
     * =======
     * +++++++ REPLACE
     */
    private static final Pattern SEARCH_REPLACE_PATTERN_LOOSE =
            Pattern.compile("-{7,}\\s*SEARCH\\s*\\R([\\s\\S]*?)\\R={7,}\\s*\\R([\\s\\S]*?)\\R?\\+{7,}\\s*REPLACE");

    /**
     * 空白字符正则（用于标准化换行符）
     */
    private static final Pattern CRLF_PATTERN = Pattern.compile("\\r\\n");
    private static final Pattern CR_PATTERN = Pattern.compile("\\r");

    /**
     * 多个空白字符正则（用于模糊匹配）
     */
    private static final Pattern MULTI_WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /**
     * 标点符号周围空格正则（用于模糊匹配）
     */
    private static final Pattern PUNCTUATION_SPACE_PATTERN = Pattern.compile("\\s*([{}();:,])\\s*");

    private CodeIncrementalUpdater() {
    }

    /**
     * 获取单例实例
     */
    public static CodeIncrementalUpdater getInstance() {
        return INSTANCE;
    }

    // ==================== 主入口方法 ====================

    /**
     * 主入口：应用增量更新
     *
     * @param diffContent 增量更新指令（可能被代码块包裹，如 ```diff ... ```）
     * @param source      原始代码（可能被代码块包裹，如 ```vue ... ```）
     * @return 更新结果
     */
    public UpdateResult apply(String diffContent, String source) {
        if (diffContent == null || diffContent.isBlank()) {
            return UpdateResult.failure(source, "增量更新指令不能为空");
        }
        if (source == null) {
            source = "";
        }

        // 1. 提取纯代码内容（处理可能的代码块包裹）
        String cleanDiffContent = extractCodeContent(diffContent);
        String cleanSource = extractCodeContent(source);

        // 2. 解析增量更新指令
        IncrementalUpdate update = parseIncrementalUpdate(cleanDiffContent);
        if (update == null) {
            // 如果没有解析到 SEARCH/REPLACE，将整个内容视为全量代码
            return UpdateResult.builder()
                    .success(true)
                    .updatedCode(cleanDiffContent)
                    .appliedUpdate(IncrementalUpdate.fullCode(cleanDiffContent))
                    .build();
        }

        // 3. 应用增量更新
        return applyIncrementalUpdate(cleanSource, update);
    }

    // ==================== 参数健壮性处理 ====================

    /**
     * 提取代码块中的纯代码内容
     * 如果被 ``` 包裹则提取内部内容，否则原样返回
     *
     * @param content 输入内容
     * @return 提取后的纯代码内容
     */
    private String extractCodeContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return content;
    }

    // ==================== 解析方法 ====================

    /**
     * 解析增量更新指令
     * 支持：1. SEARCH/REPLACE 格式 2. 完整代码
     *
     * @param aiResponse AI 返回的内容
     * @return 增量更新结构，如果无有效内容则返回 null
     */
    public IncrementalUpdate parseIncrementalUpdate(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return null;
        }

        // 尝试提取 SEARCH/REPLACE 部分
        return extractPureSearchReplace(aiResponse);
    }

    /**
     * 提取纯 SEARCH/REPLACE 部分
     *
     * @param content 输入内容
     * @return 增量更新结构，如果没有匹配则返回 null
     */
    private IncrementalUpdate extractPureSearchReplace(String content) {
        List<SearchReplaceItem> matches = new ArrayList<>();
        Matcher matcher = SEARCH_REPLACE_PATTERN.matcher(content);

        // 先用严格的模式匹配
        while (matcher.find()) {
            String search = matcher.group(1).trim();
            String replace = matcher.group(2).trim();
            matches.add(new SearchReplaceItem(search, replace));
        }

        // 如果严格模式匹配不到，尝试宽松模式
        if (matches.isEmpty()) {
            matcher = SEARCH_REPLACE_PATTERN_LOOSE.matcher(content);
            while (matcher.find()) {
                String search = matcher.group(1).trim();
                String replace = matcher.group(2).trim();
                matches.add(new SearchReplaceItem(search, replace));
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        return IncrementalUpdate.searchReplace(matches);
    }

    // ==================== 应用方法 ====================

    /**
     * 应用增量更新到现有代码
     *
     * @param currentCode 当前代码
     * @param update      增量更新结构
     * @return 更新结果
     */
    public UpdateResult applyIncrementalUpdate(String currentCode, IncrementalUpdate update) {
        try {
            if (update.isSearchReplace() && update.getUpdates() != null && !update.getUpdates().isEmpty()) {
                // 应用一个或多个 SEARCH/REPLACE 更新
                String updatedCode = currentCode;
                List<SearchReplaceItem> appliedUpdates = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                int totalMatchCount = 0;

                List<SearchReplaceItem> updates = update.getUpdates();
                for (int i = 0; i < updates.size(); i++) {
                    SearchReplaceItem item = updates.get(i);
                    MatchOptions options = MatchOptions.builder()
                            .requireUniqueMatch(true)
                            .allowFuzzyMatch(true)
                            .build();

                    UpdateResult result = applySearchReplace(updatedCode, item.search(), item.replace(), options);

                    if (result.isSuccess()) {
                        updatedCode = result.getUpdatedCode();
                        appliedUpdates.add(item);
                        totalMatchCount += result.getMatchCount() != null ? result.getMatchCount() : 0;
                    } else {
                        errors.add("更新 " + (i + 1) + " 失败: " + result.getError());
                    }
                }

                if (appliedUpdates.isEmpty()) {
                    // 所有更新都失败了
                    return UpdateResult.builder()
                            .success(false)
                            .updatedCode(currentCode)
                            .error("所有更新都失败:\n" + String.join("\n", errors))
                            .matchCount(0)
                            .build();
                }

                // 检查替换后的代码是否残留 SEARCH/REPLACE 标记
                String residualError = checkResidualMarkers(updatedCode);
                if (residualError != null) {
                    return UpdateResult.builder()
                            .success(false)
                            .updatedCode(currentCode)
                            .error(residualError)
                            .matchCount(totalMatchCount)
                            .build();
                }

                if (!errors.isEmpty()) {
                    // 部分更新成功
                    return UpdateResult.builder()
                            .success(true)
                            .updatedCode(updatedCode)
                            .appliedUpdate(IncrementalUpdate.searchReplace(appliedUpdates))
                            .error("部分更新失败:\n" + String.join("\n", errors))
                            .matchCount(totalMatchCount)
                            .build();
                } else {
                    // 所有更新都成功
                    return UpdateResult.builder()
                            .success(true)
                            .updatedCode(updatedCode)
                            .appliedUpdate(IncrementalUpdate.searchReplace(appliedUpdates))
                            .matchCount(totalMatchCount)
                            .build();
                }
            } else if (update.isFullCode() && update.getFullCode() != null) {
                // 检查完整代码是否残留 SEARCH/REPLACE 标记
                String residualError = checkResidualMarkers(update.getFullCode());
                if (residualError != null) {
                    return UpdateResult.builder()
                            .success(false)
                            .updatedCode(currentCode)
                            .error(residualError)
                            .build();
                }
                // 直接使用完整代码
                return UpdateResult.builder()
                        .success(true)
                        .updatedCode(update.getFullCode())
                        .appliedUpdate(update)
                        .build();
            } else {
                return UpdateResult.failure(currentCode, "无效的更新格式");
            }
        } catch (Exception e) {
            return UpdateResult.failure(currentCode, "应用增量更新时出错：" + e.getMessage());
        }
    }

    /**
     * 检查代码中是否残留 SEARCH/REPLACE 标记
     * 如果存在残留标记，说明替换未正确完成
     *
     * @param code 要检查的代码
     * @return 如果有残留标记返回错误信息，否则返回 null
     */
    private String checkResidualMarkers(String code) {
        if (code == null) return null;

        boolean hasSearchMarker = code.contains("------- SEARCH") || code.contains("-------SEARCH");
        boolean hasReplaceMarker = code.contains("+++++++ REPLACE") || code.contains("+++++++REPLACE");

        if (hasSearchMarker && hasReplaceMarker) {
            return "替换失败：代码中残留了 SEARCH/REPLACE 标记，增量更新指令未被正确应用";
        } else if (hasSearchMarker) {
            return "替换失败：代码中残留了 '------- SEARCH' 标记，增量更新指令未被正确应用";
        } else if (hasReplaceMarker) {
            return "替换失败：代码中残留了 '+++++++ REPLACE' 标记，增量更新指令未被正确应用";
        }

        return null;
    }

    /**
     * 应用单次 SEARCH/REPLACE 更新
     *
     * @param code    原始代码
     * @param search  要搜索的内容
     * @param replace 要替换的内容
     * @param options 匹配选项
     * @return 更新结果
     */
    public UpdateResult applySearchReplace(String code, String search, String replace, MatchOptions options) {
        try {
            if (options == null) {
                options = MatchOptions.defaults();
            }

            // 标准化换行符
            String normalizedCode = normalizeLineEndings(code);
            String normalizedSearch = normalizeLineEndings(search);
            String normalizedReplace = normalizeLineEndings(replace);

            // 查找所有匹配位置
            List<MatchResult> matches = findAllMatches(normalizedCode, normalizedSearch, options);

            if (matches.isEmpty()) {
                String searchPreview = search.length() > 200
                        ? search.substring(0, 200) + "..."
                        : search;
                return UpdateResult.builder()
                        .success(false)
                        .updatedCode(code)
                        .error("未找到匹配的代码片段。搜索内容：\n" + searchPreview)
                        .matchCount(0)
                        .build();
            }

            // 检查是否要求唯一匹配
            if (options.isRequireUniqueMatch() && matches.size() > 1) {
                StringBuilder positions = new StringBuilder();
                for (int i = 0; i < matches.size(); i++) {
                    if (i > 0) positions.append(", ");
                    positions.append(matches.get(i).getIndex());
                }
                return UpdateResult.builder()
                        .success(false)
                        .updatedCode(code)
                        .error("找到 " + matches.size() + " 个匹配位置（位置：" + positions +
                                "），无法确定要替换哪一个。请提供更多上下文以确保唯一匹配。")
                        .matchCount(matches.size())
                        .build();
            }

            // 使用第一个匹配
            MatchResult bestMatch = matches.getFirst();

            // 执行替换
            String beforeMatch = normalizedCode.substring(0, bestMatch.getIndex());
            String afterMatch = normalizedCode.substring(bestMatch.getIndex() + bestMatch.getLength());
            String updatedCode = beforeMatch + normalizedReplace + afterMatch;

            // 验证替换是否成功
            if (updatedCode.equals(normalizedCode) && !search.isEmpty()) {
                return UpdateResult.builder()
                        .success(false)
                        .updatedCode(code)
                        .error("替换未生效，请检查SEARCH内容是否精确匹配")
                        .matchCount(matches.size())
                        .build();
            }

            return UpdateResult.builder()
                    .success(true)
                    .updatedCode(updatedCode)
                    .appliedUpdate(IncrementalUpdate.searchReplace(List.of(new SearchReplaceItem(search, replace))))
                    .matchCount(matches.size())
                    .build();
        } catch (Exception e) {
            return UpdateResult.builder()
                    .success(false)
                    .updatedCode(code)
                    .error("应用更新时出错：" + e.getMessage())
                    .matchCount(0)
                    .build();
        }
    }

    // ==================== 匹配算法 ====================

    /**
     * 查找所有匹配位置，支持上下文感知和模糊匹配
     *
     * @param code    代码内容
     * @param search  搜索内容
     * @param options 匹配选项
     * @return 匹配结果列表
     */
    private List<MatchResult> findAllMatches(String code, String search, MatchOptions options) {
        List<MatchResult> matches = new ArrayList<>();
        int contextLines = options != null ? options.getContextLines() : 3;
        boolean allowFuzzy = options == null || options.isAllowFuzzyMatch();

        // 1. 首先尝试精确匹配
        int startIndex = 0;
        while (true) {
            int index = code.indexOf(search, startIndex);
            if (index == -1) break;

            // 提取上下文
            String context = extractContext(code, index, search.length(), contextLines);

            matches.add(new MatchResult(
                    index,
                    search.length(),
                    context,
                    matches.isEmpty()  // 如果是第一个匹配，暂时认为是唯一的
            ));

            startIndex = index + 1;  // 继续查找下一个匹配
        }

        // 2. 如果没有精确匹配，尝试模糊匹配
        if (matches.isEmpty() && allowFuzzy) {
            findFuzzyMatches(code, search, contextLines, matches);
        }

        // 3. 去重并更新 isUnique 属性
        return deduplicateAndUpdateMatches(matches);
    }

    /**
     * 查找模糊匹配
     * 通过移除多余空白来进行模糊匹配
     *
     * @param code         原始代码
     * @param search       搜索内容
     * @param contextLines 上下文行数
     * @param matches      匹配结果列表（输出参数）
     */
    private void findFuzzyMatches(String code, String search, int contextLines, List<MatchResult> matches) {
        // 简化搜索内容：移除多余空白
        String simplifiedSearch = removeExtraWhitespace(search);
        String simplifiedCode = removeExtraWhitespace(code);

        // 在简化后的代码中查找
        int startIndex = 0;
        while (true) {
            int index = simplifiedCode.indexOf(simplifiedSearch, startIndex);
            if (index == -1) break;

            // 将简化代码中的位置映射回原始代码
            int originalIndex = mapToOriginalPosition(code, simplifiedCode, index);
            if (originalIndex != -1) {
                String context = extractContext(code, originalIndex, search.length(), contextLines);
                matches.add(new MatchResult(
                        originalIndex,
                        search.length(),
                        context,
                        false  // 将在去重后更新
                ));
            }

            startIndex = index + 1;
        }
    }

    /**
     * 提取代码上下文
     *
     * @param code         原始代码
     * @param startIndex   匹配开始位置
     * @param length       匹配长度
     * @param contextLines 上下文行数
     * @return 带标记的上下文字符串
     */
    private String extractContext(String code, int startIndex, int length, int contextLines) {
        String[] lines = code.split("\n", -1);
        int currentPos = 0;
        int targetLine = -1;
        int targetColumn = -1;

        // 找到目标行和列
        for (int i = 0; i < lines.length; i++) {
            int lineLength = lines[i].length() + 1;  // +1 for newline
            if (startIndex >= currentPos && startIndex < currentPos + lineLength) {
                targetLine = i;
                targetColumn = startIndex - currentPos;
                break;
            }
            currentPos += lineLength;
        }

        if (targetLine == -1) return "";

        // 提取上下文行
        int startLine = Math.max(0, targetLine - contextLines);
        int endLine = Math.min(lines.length - 1, targetLine + contextLines);

        List<String> contextLinesArray = new ArrayList<>();
        for (int i = startLine; i <= endLine; i++) {
            String line = lines[i];

            if (i == targetLine) {
                // 标记目标行和匹配范围
                String prefix = ">>> ";
                String beforeMatch = line.substring(0, Math.min(targetColumn, line.length()));
                int matchEndInLine = Math.min(targetColumn + length, line.length());
                String matchText = targetColumn < line.length()
                        ? line.substring(targetColumn, matchEndInLine)
                        : "";
                String afterMatch = matchEndInLine < line.length()
                        ? line.substring(matchEndInLine)
                        : "";

                // 如果匹配文本在当前行内
                if (matchText.length() == length) {
                    // 完整匹配在当前行
                    line = prefix + beforeMatch + "[" + matchText + "]" + afterMatch;
                } else if (!matchText.isEmpty()) {
                    // 部分匹配在当前行（跨行匹配）
                    line = prefix + beforeMatch + "[" + matchText + "...]" + afterMatch;
                } else {
                    // 匹配在当前行开始但跨越多行
                    line = prefix + beforeMatch + "[...]";
                }
            } else {
                line = "    " + line;
            }
            contextLinesArray.add(line);
        }

        return String.join("\n", contextLinesArray);
    }

    /**
     * 将简化代码中的位置映射回原始代码
     *
     * @param originalCode   原始代码
     * @param simplifiedCode 简化后的代码
     * @param simplifiedIndex 简化代码中的位置
     * @return 原始代码中的位置，如果无法映射则返回 -1
     */
    private int mapToOriginalPosition(String originalCode, String simplifiedCode, int simplifiedIndex) {
        int originalPos = 0;
        int simplifiedPos = 0;

        while (simplifiedPos < simplifiedIndex && originalPos < originalCode.length()) {
            char originalChar = originalCode.charAt(originalPos);
            char simplifiedChar = simplifiedCode.charAt(simplifiedPos);

            if (originalChar == simplifiedChar) {
                originalPos++;
                simplifiedPos++;
            } else if (Character.isWhitespace(originalChar)) {
                // 原始代码中的空白字符在简化代码中被移除
                originalPos++;
            } else {
                // 字符不匹配，无法映射
                return -1;
            }
        }

        return originalPos;
    }

    /**
     * 去重并更新匹配结果
     *
     * @param matches 原始匹配结果列表
     * @return 去重后的匹配结果列表
     */
    private List<MatchResult> deduplicateAndUpdateMatches(List<MatchResult> matches) {
        if (matches.isEmpty()) return matches;

        List<MatchResult> uniqueMatches = new ArrayList<>();
        Set<Integer> seenIndices = new HashSet<>();

        for (MatchResult match : matches) {
            if (!seenIndices.contains(match.getIndex())) {
                seenIndices.add(match.getIndex());
                uniqueMatches.add(match);
            }
        }

        // 更新 isUnique 属性
        boolean isUnique = uniqueMatches.size() == 1;
        for (MatchResult match : uniqueMatches) {
            match.setUnique(isUnique);
        }

        return uniqueMatches;
    }

    // ==================== 工具方法 ====================

    /**
     * 标准化换行符
     * 将 \r\n 和 \r 统一转换为 \n
     *
     * @param text 输入文本
     * @return 标准化后的文本
     */
    private String normalizeLineEndings(String text) {
        if (text == null) return null;
        return CR_PATTERN.matcher(CRLF_PATTERN.matcher(text).replaceAll("\n")).replaceAll("\n");
    }

    /**
     * 移除多余的空白字符（用于模糊匹配）
     *
     * @param text 输入文本
     * @return 处理后的文本
     */
    private String removeExtraWhitespace(String text) {
        if (text == null) return null;
        // 多个空白字符替换为单个空格
        String result = MULTI_WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
        // 移除标点符号周围的空格
        result = PUNCTUATION_SPACE_PATTERN.matcher(result).replaceAll("$1");
        return result.trim();
    }
}
