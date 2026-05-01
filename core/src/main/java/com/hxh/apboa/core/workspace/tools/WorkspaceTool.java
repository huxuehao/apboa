package com.hxh.apboa.core.workspace.tools;

import com.hxh.apboa.common.consts.SysConst;
import com.hxh.apboa.common.r.R;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.tool.Tool;

import java.nio.file.Paths;

/**
 * 描述：获取当前工作目录工具。返回当前会话的专属工作目录绝对路径，
 * AI 所有文件操作（创建、读取、修改、删除）都必须限定在此目录下。
 *
 * @author huxuehao
 **/
public class WorkspaceTool {

    /**
     * 获取当前工作目录的绝对路径。
     * <p>
     * 工作目录路径规则：{当前运行目录}/.apboa/workspace/units/{session_id}
     * 使用 java.nio.file.Paths 拼接路径，自动兼容 Windows 和 Unix 操作系统。
     *
     * @return 工作目录的绝对路径
     */
    @Tool(
            name = "get_workspace_directory",
            description = "获取当前工作目录的绝对路径。这是你的专属工作空间根目录，"
                        + "你只能在此目录及其子目录下操作文件。"
                        + "所有文件的创建、读取、编辑、删除等操作，都必须限定在此目录范围内。"
                        + "严禁访问或操作此目录以外的任何文件。此工具无需任何参数。"
    )
    public Object getWorkspaceDirectory(AgentContext agentContext) {
        // 获取当前运行目录（user.dir），自动兼容各操作系统
        String currentDir = System.getProperty("user.dir");
        // 使用 Paths 拼接路径，自动处理 Windows/Unix 分隔符差异
        String workspacePath = Paths.get(currentDir,
                        SysConst.ROOT_DIR_NAME,
                        SysConst.WORKSPACE_DIR_NAME,
                        SysConst.UNITS_DIR_NAME,
                        agentContext.getThreadId())
                .toAbsolutePath()
                .normalize()
                .toString();
        return R.data(workspacePath);
    }
}
