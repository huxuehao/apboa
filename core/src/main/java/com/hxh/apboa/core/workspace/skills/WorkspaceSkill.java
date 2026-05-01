package com.hxh.apboa.core.workspace.skills;

import com.hxh.apboa.common.consts.SysConst;
import io.agentscope.core.skill.AgentSkill;

/**
 * 描述：该技能用于告知智能体存在工作区目录
 * <p>
 * 优化点：
 * 1. 抽取路径常量，避免魔法字符串
 * 2. 明确区分“命令执行目录”和“文件操作根目录”
 * 3. 详细说明技能脚本的引用规则与参数路径修正要求
 * 4. 提供大量正误示例，降低模型幻觉
 *
 * @author huxuehao
 **/
public class WorkspaceSkill {

    static final String WORKSPACE_ROOT = SysConst.WORKSPACE_PATH;   // 例如 ".apboa/workspace"
    static final String SKILLS_DIR = SysConst.SKILLS_DIR_NAME;
    static final String UNITS_DIR = SysConst.UNITS_DIR_NAME;
    static final String SESSION_PLACEHOLDER = "{session_id}";

    static final String SKILL_NAME = "workspace_directory";

    /**
     * 构建静态的工作空间目录规范技能（默认版本，不包含具体 session 信息）
     */
    public static AgentSkill getAgentSkill() {
        return buildSkill(null);
    }

    /**
     * 扩展点：可根据当前 sessionId 构建包含具体工作单元路径的技能描述
     *
     * @param sessionId 当前会话 ID，为 null 时使用占位符
     */
    public static AgentSkill getAgentSkill(String sessionId) {
        return buildSkill(sessionId);
    }

    private static AgentSkill buildSkill(String sessionId) {
        String effectiveSession = sessionId != null ? sessionId : SESSION_PLACEHOLDER;
        return AgentSkill.builder()
                .name(SKILL_NAME)
                .description(buildDescription())
                .skillContent(buildSkillContent(effectiveSession))
                .build();
    }

    private static String buildDescription() {
        return "工作空间目录与执行规范。"
                + "本技能定义了智能体在操作文件（创建、读取、修改、删除）和执行 Shell 命令时，"
                + "必须遵守的工作空间目录结构、路径使用规则以及技能脚本执行规范。"
                + "任何文件操作必须限定在会话专属工作单元内，任何技能脚本引用必须使用正确的相对路径。";
    }

    /**
     * 生成详细的技能内容，重点解释三个核心概念：
     * 1. 目录结构
     * 2. 两个不同的“根”概念（命令执行根 vs 文件操作根）
     * 3. 技能脚本执行规范（路径前缀、参数修正）
     */
    private static String buildSkillContent(String sessionId) {
        return "# 工作空间目录与执行规范\n\n"

                + "## 一、目录结构\n\n"
                + "整个工作空间位于项目根目录下的 `" + WORKSPACE_ROOT + "/` 内，结构如下：\n\n"
                + "```\n"
                + WORKSPACE_ROOT + "/\n"
                + "├── " + SKILLS_DIR + "/                  # 全局技能目录（只读）\n"
                + "│   ├── doGetCurrentTime/\n"
                + "│   └── <其他技能>/\n"
                + "└── " + UNITS_DIR + "/                    # 工作单元根目录\n"
                + "    ├── " + sessionId + "/         # 当前会话专属工作单元（可读写）\n"
                + "    └── ...\n"
                + "```\n\n"

                + "## 二、两个核心“根”概念（务必分清）\n\n"
                + "### 1. 命令执行根目录（Shell 命令的当前工作目录）\n"
                + "- **所有 Shell 命令都是在 `" + WORKSPACE_ROOT + "/` 目录下执行的**。\n"
                + "- 也就是说，命令的“当前目录”就是工作空间根目录本身。\n"
                + "- 这一点直接影响技能脚本的路径写法（见下一节）。\n\n"
                + "### 2. 文件操作根目录（文件读写的专属区域）\n"
                + "- 通过调用 `get_workspace_directory` 工具，你会获得当前会话工作单元的**绝对路径**，"
                + "例如 `/app/" + WORKSPACE_ROOT + "/" + UNITS_DIR + "/" + sessionId + "/`。\n"
                + "- **你在本次对话中创建、修改、查看、删除的所有文件，都必须位于这个工作单元目录内**。\n"
                + "- 你**严禁**直接操作工作单元以外的任何路径（包括 `" + SKILLS_DIR + "/` 目录下的任何写入）。\n"
                + "- 构造文件路径时，必须从 `get_workspace_directory` 返回的绝对路径开始拼接。\n\n"

                + "## 三、技能脚本执行规范（极其重要）\n\n"
                + "### 规则 A：引用技能脚本必须带 `" + SKILLS_DIR + "/` 前缀\n"
                + "因为命令在 `" + WORKSPACE_ROOT + "/` 下执行，所以调用技能目录中的脚本时，"
                + "**必须**写成相对路径 `" + SKILLS_DIR + "/技能名/脚本`，而不能直接写技能名。\n\n"
                + "**正确示例：**\n"
                + "```shell\n"
                + "python " + SKILLS_DIR + "/doGetCurrentTime/scripts/getCurrentTime.py\n"
                + "```\n\n"
                + "**错误示例（严禁）：**\n"
                + "```shell\n"
                + "python doGetCurrentTime/scripts/getCurrentTime.py          # 缺少 " + SKILLS_DIR + "/ 前缀\n"
                + "python /absolute/path/to/" + SKILLS_DIR + "/doGetCurrentTime/scripts/getCurrentTime.py  # 禁止使用绝对路径\n"
                + "```\n\n"

                + "### 规则 B：脚本的文件参数必须落在工作单元内\n"
                + "当技能脚本需要操作文件时（例如创建文件、读取文件），**任何作为参数传递的文件路径，"
                + "都必须指向当前会话工作单元 `" + UNITS_DIR + "/" + sessionId + "/` 下的位置**。\n\n"
                + "你可以直接使用 `get_workspace_directory` 返回的绝对路径，拼接文件名；\n"
                + "也可以使用相对于命令执行目录的路径：`" + UNITS_DIR + "/" + sessionId + "/文件名`。\n\n"
                + "**正确示例：**\n"
                + "```shell\n"
                + "# 假设 get_workspace_directory 返回 /app/" + WORKSPACE_ROOT + "/" + UNITS_DIR + "/" + sessionId + "/\n"
                + "python " + SKILLS_DIR + "/someSkill/scripts/generate.py -path /app/" + WORKSPACE_ROOT + "/" + UNITS_DIR + "/" + sessionId + "/result.txt\n"
                + "# 或者使用相对形式（同样正确）\n"
                + "python " + SKILLS_DIR + "/someSkill/scripts/generate.py -path " + UNITS_DIR + "/" + sessionId + "/result.txt\n"
                + "```\n\n"
                + "**错误示例（严禁）：**\n"
                + "```shell\n"
                + "python " + SKILLS_DIR + "/someSkill/scripts/generate.py -path result.txt          # 相对路径 result.txt 会被解析到 " + WORKSPACE_ROOT + "/result.txt，逃逸出工作单元！\n"
                + "python " + SKILLS_DIR + "/someSkill/scripts/generate.py -path /tmp/output.txt       # 绝对路径指向外部，禁止\n"
                + "python " + SKILLS_DIR + "/someSkill/scripts/generate.py -path " + SKILLS_DIR + "/config.json   # 试图写入技能目录，禁止\n"
                + "```\n\n"

                + "## 四、文件操作通用规范（重申）\n\n"
                + "- 进行任何文件操作（view, list, insert, write）时，路径参数必须是以工作单元为基底的绝对路径（通过 `get_workspace_directory` 拼接获得）。\n"
                + "- 禁止使用 `../` 跳出工作单元。\n"
                + "- 禁止访问工作空间以外的任何路径。\n"
                + "- 技能目录 `" + SKILLS_DIR + "/` 是只读的，只能读取技能脚本，不能写入、修改或删除任何内容。\n\n"

                + "## 五、快速自查清单\n\n"
                + "在生成任何 Shell 命令或文件路径前，请自问：\n"
                + "1. 这条命令的执行目录是不是 `" + WORKSPACE_ROOT + "/` ？如果是，我的技能脚本路径是否以 `" + SKILLS_DIR + "/` 开头？\n"
                + "2. 我的文件操作目标是否在 `" + UNITS_DIR + "/" + sessionId + "/` 内？\n"
                + "3. 我是否使用了 `get_workspace_directory` 的返回值来构造绝对路径？\n"
                + "任何违反以上规则的行为都将被系统强制拦截或纠正。";
    }
}
