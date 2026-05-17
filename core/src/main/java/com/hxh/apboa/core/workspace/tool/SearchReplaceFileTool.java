package com.hxh.apboa.core.workspace.tool;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.core.agui.AgentContext;
import com.hxh.apboa.core.mpatch.core.CodeIncrementalUpdater;
import com.hxh.apboa.core.mpatch.model.UpdateResult;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 描述：文件搜索替换工具
 * 根据 SEARCH/REPLACE 指令对目标文件执行增量搜索替换操作
 *
 * @author huxuehao
 **/
public class SearchReplaceFileTool {
    private static final Logger logger = LoggerFactory.getLogger(SearchReplaceFileTool.class);

    private final CodeIncrementalUpdater updater = CodeIncrementalUpdater.getInstance();

    @Tool(
            name = "search_replace_file",
            description =
                    "Performs precise incremental text editing on a file using SEARCH/REPLACE blocks."
                            + " Reads the file at file_path, applies all SEARCH/REPLACE transformations"
                            + " provided in content, then writes the updated content back to the same file."
                            + " Supports insert, modify, and delete operations on both code and plain text files."
                            + " Recommended when the modification scope is less than 30% of the file;"
                            + " not recommended for large-scale rewrites exceeding 30% of the file content."
                            + " For the content format, refer to the search_replace_skill_guide skill.")
    public Object searchReplaceFile(
            @ToolParam(name = "file_path", description = "The target file path to edit") String filePath,
            @ToolParam(name = "content", description = "One or more SEARCH/REPLACE blocks defining the edits to apply") String content,
            AgentContext agentContext) {
        // 参数校验
        if (filePath == null || filePath.isBlank()) {
            return "Failure: file_path must not be empty";
        }
        if (content == null || content.isBlank()) {
            return "Failure: content must not be empty";
        }

        if (agentContext == null) {
            return "Failure: agentContext must not be empty";
        }

        // 基于 workDir 构造工作目录路径
        Path workDirPath = Paths.get(SysConst.WORKSPACE_PATH, agentContext.getThreadId()).normalize();

        // 基于 workDir 解析文件路径，防止路径穿越
        Path file = workDirPath.resolve(filePath).normalize();
        if (!file.startsWith(workDirPath)) {
            return "Failure: invalid file path, must stay within workspace";
        }

        // 文件存在性检查
        if (!Files.exists(file)) {
            return "Failure: file not found: " + file;
        }
        if (!Files.isRegularFile(file)) {
            return "Failure: path is not a regular file: " + file;
        }

        // 读取源文件内容
        String source;
        try {
            source = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取文件失败: {}", file, e);
            return "Failure: unable to read file: " + e.getMessage();
        }

        // 应用增量更新
        UpdateResult result = updater.apply(content, source);
        if (!result.isSuccess()) {
            return "Failure: " + result.getError();
        }

        String newSource = result.getUpdatedCode();

        // 将更新后的内容写回文件
        try {
            Files.writeString(file, newSource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("写入文件失败: {}", file, e);
            return "Failure: unable to write file: " + e.getMessage();
        }

        logger.info("文件更新成功: {}", file);
        return "Success";
    }
}
