package com.hxh.apboa.core.workspace.skills;

import io.agentscope.core.skill.AgentSkill;

public class WorkspaceSkill {

    private static final String SKILL_NAME = "workspace_path_and_execution_rules";

    public static AgentSkill getAgentSkill() {
        return AgentSkill.builder()
                .name(SKILL_NAME)
                .description(
                        "当你需要执行以下任何操作时，必须调用此技能获取正确的路径格式：\n"
                        + "1. 使用文件工具（view_text_file、list_directory、insert_text_file、write_text_file 等）\n"
                        + "2. 使用 execute_shell_command 工具执行 Shell 命令（cat、ls、python、bash、node 等）\n"
                        + "3. 调用任何SKILL脚本（如 doGetCurrentTime、csvAnalyzer 等）\n"
                        + "4. 命令或工具参数中包含文件路径\n\n"
                        + "【重要】在执行任何涉及路径的操作前，建议先调用此技能，避免被系统拦截。"
                )
                .skillContent(buildSkillContent())
                .build();
    }

    private static String buildSkillContent() {
        return """
            # 工作空间路径与执行规范

            ## 一、核心原则：一切路径使用相对路径

            你当前的工作目录就是你的**专属会话工作空间**。所有操作都必须基于此目录进行。
            - 文件工具（view_text_file、list_directory、insert_text_file、write_text_file、execute_shell_command 等）的路径参数只能写**相对路径**。
            - Shell 命令中的所有路径也必须是**相对路径**。
            - **绝对路径**（以 `/` 或盘符开头）以及** `../` 逃逸**（除技能脚本的特殊前缀外）全部被系统拦截。

            ## 二、文件操作规范

            所有文件操作都限定在你的工作空间内，你只需使用简单的相对路径：

            ```text
            view_text_file(file_path="report.md")
            list_directory(dir_path=".")
            insert_text_file(file_path="./data.md")
            write_text_file(file_path="data.md")
            ```

            禁止：
            ```text
            view_text_file(file_path="/etc/report.md") // 绝对路径
            view_text_file(file_path="../report.md") // .. 逃逸
            list_directory(dir_path="/other/dir") // 绝对路径
            list_directory(dir_path="../../") // .. 逃逸
            insert_text_file(file_path="/data.md") // 绝对路径
            insert_text_file(file_path="../data.md") // .. 逃逸
            write_text_file(file_path="/other/secret.txt")  // 绝对路径
            write_text_file(file_path="../other/secret.txt")  // .. 逃逸
            ```

            ## 三、技能脚本调用规范

            预置的技能脚本存放在 `skills/` 目录下，但因为你的当前目录在 `workspaces/你的会话/` 内，
            所以你必须使用 **`../../skills/`** 前缀来访问它们。

            正确格式：`../../skills/<技能名>/scripts/<脚本文件名>`

            **正确示例：**
            ```shell
            python ../../skills/doGetCurrentTime/scripts/getCurrentTime.py
            bash ../../skills/textProcessor/scripts/clean.sh
            node ../../skills/imageResizer/scripts/resize.js --width 800 --height 600
            ```

            错误示例：
            ```shell
            python skills/doGetCurrentTime/scripts/getCurrentTime.py   # 缺少 ../
            python /absolute/path/to/skills/...                        # 绝对路径
            python ../../evil/script.py                                # 跳出工作空间
            ```

            ## 四、脚本参数也必须是相对路径

            当技能脚本需要文件参数时，你必须使用工作空间内的相对路径，例如：
            ```shell
            python ../../skills/csvAnalyzer/scripts/analyze.py data/input.csv --out result.json
            ```
            绝不允许将输出路径指向 `skills/` 目录或外部。

            ## 五、内联代码执行

            如果你使用 `python -c "..."` 或 `node -e "..."` 等内联方式执行代码，**代码内部的文件路径**也必须遵守相对路径规则，系统会检查代码内容。

            ## 六、快速自查

            1. 路径是否以 `/` 或盘符开头？ → 改成相对路径。
            2. 是否使用了 `../` ？ → 只有 `../../skills/...` 被允许，其他一律禁止。
            3. 技能脚本调用是否以 `../../skills/` 开头？ → 是，否则会失败。

            违反规则的操作会立即被拦截并提示你学习本技能。
            """;
    }
}
