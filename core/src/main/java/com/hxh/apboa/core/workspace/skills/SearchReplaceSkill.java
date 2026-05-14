package com.hxh.apboa.core.workspace.skills;

import io.agentscope.core.skill.AgentSkill;

/**
 * 描述：基于SEARCH与REPLACE机制的增量更新指南
 *
 * @author huxuehao
 **/
public class SearchReplaceSkill {
    private static final String SKILL_NAME = "search_replace_skill_guide";

    public static AgentSkill getAgentSkill() {
        return AgentSkill.builder()
                .name(SKILL_NAME)
                .description(
                        "Search and replace file skill guide. "
                                + "Before calling the search_replace_file tool to perform incremental editing on a file, "
                                + "you must first invoke this skill to learn the correct format of SEARCH/REPLACE instructions. "
                                + "This skill covers six operation modes: insert (single/multiple), modify (single/multiple), "
                                + "delete (single/multiple), along with mandatory format requirements and usage recommendations "
                                + "to ensure precise and reliable editing operations."
                )
                .skillContent(buildSkillContent())
                .build();
    }

    private static String buildSkillContent() {
        return """
            # Skill Name
            Incremental Update Guide Based on SEARCH/REPLACE Mechanism

            ## Overview

            This skill teaches how to perform precise incremental editing on text files using the **SEARCH/REPLACE** pattern.
            The method works by matching a specific code snippet in the original file and replacing it with new content,
            enabling insert, modify, or delete operations on text.

            ## Core Mechanism

            Each editing operation consists of a pair of **SEARCH** and **REPLACE** markers:

            ## SEARCH/REPLACE Instruction Format

            Instructions must follow this format:

            ```
            ------- SEARCH
            (exact snippet currently present in the source file)
            =======
            (modified snippet)
            +++++++ REPLACE
            ```

            ## Original Content

            Assume the original content looks roughly like:

            ```
            function init() {
             console.log('init');
            }

            function fetchData() {
             return api.get('/list');
            }

            function destroy() {
             console.log('destroy');
            }
            ```

            ## Six Operation Modes

            ### 1. Single Insert (add code at one location)

            **Goal**: Add a `render` function after `fetchData`.

            ```
            ------- SEARCH
            function fetchData() {
             return api.get('/list');
            }
            =======
            function fetchData() {
             return api.get('/list');
            }

            function render() {
             console.log('render');
            }
            +++++++ REPLACE
            ```

            Explanation:

            - `SEARCH`: a snippet that already exists in the file and can be uniquely matched
            - `REPLACE`: the original code preserved plus the new `render` function added (equivalent to "insert after")

            To insert before, simply place the new function above the original code.

            ### 2. Multiple Insert (add code at multiple locations)

            **Goal**:

            - Add `setupLogger` after `init`
            - Add `cleanup` before `destroy`

            ```
            ------- SEARCH
            function init() {
             console.log('init');
            }
            =======
            function init() {
             console.log('init');
            }

            function setupLogger() {
             console.log('logger setup');
            }
            +++++++ REPLACE

            ------- SEARCH
            function destroy() {
             console.log('destroy');
            }
            =======
            function cleanup() {
             console.log('cleanup');
            }

            function destroy() {
             console.log('destroy');
            }
            +++++++ REPLACE
            ```

            Explanation:

            - Two **SEARCH/REPLACE** pairs are written in the same code block:
               - The first pair inserts `setupLogger` after `init`
               - The second pair inserts `cleanup` before `destroy`
            - The engine executes these two updates sequentially.

            ### 3. Single Modify (edit one location)

            **Goal**: Change the implementation logic of `fetchData`.

            Original:

            ```
            function fetchData() {
             return api.get('/list');
            }
            ```

            Incremental instruction:

            ```
            ------- SEARCH
            function fetchData() {
             return api.get('/list');
            }
            =======
            function fetchData() {
             const params = { page: 1, pageSize: 20 };
             return api.get('/list', { params });
            }
            +++++++ REPLACE
            ```

            Explanation:

            - `SEARCH`: paste the complete original function
            - `REPLACE`: write the modified function to achieve "full replacement"

            ### 4. Multiple Modify (edit multiple locations)

            **Goal**: Modify both `init` and `destroy` functions simultaneously.

            Original:

            ```
            function init() {
             console.log('init');
            }

            function destroy() {
             console.log('destroy');
            }
            ```

            Incremental instruction:

            ```
            ------- SEARCH
            function init() {
             console.log('init');
            }
            =======
            function init() {
             console.log('app init...');
             setupLogger();
            }
            +++++++ REPLACE

            ------- SEARCH
            function destroy() {
             console.log('destroy');
            }
            =======
            function destroy() {
             console.log('app destroy...');
             cleanup();
            }
            +++++++ REPLACE
            ```

            Explanation:

            - First pair replaces `init`
            - Second pair replaces `destroy`
            - Both modifications are applied sequentially to the same file.

            ### 5. Single Delete (delete one location)

            **Goal**: Delete the `destroy` function.

            Original:

            ```
            function destroy() {
             console.log('destroy');
            }
            ```

            Incremental instruction:

            ```
            ------- SEARCH
            function destroy() {
             console.log('destroy');
            }
            =======

            +++++++ REPLACE
            ```

            Explanation:

            - `SEARCH`: list the complete code to be deleted
            - `REPLACE`: leave empty (do not write any code) — the effect is "delete the entire block"

            ### 6. Multiple Delete (delete multiple locations)

            **Goal**: Delete both the `destroy` function and a "debug variable" simultaneously.

            Assume the original code also contains:

            ```
            const debug = true;

            ....

            function destroy() {
             console.log('destroy');
            }
            ```

            Incremental instruction:

            ```
            ------- SEARCH
            const debug = true;
            =======

            +++++++ REPLACE

            ------- SEARCH
            function destroy() {
             console.log('destroy');
            }
            =======

            +++++++ REPLACE
            ```

            Explanation:

            - First pair: replace `const debug = true;` with empty → delete
            - Second pair: replace the entire `destroy` function with empty → delete
            - Note: for delete scenarios, there must be a blank line between `=======` and `+++++++ REPLACE`

            ## Supplement: Incremental Updates for Plain Text Documents

            The SEARCH/REPLACE pattern above applies not only to code files but also to **any plain text document**
            for precise incremental editing, including:

            - **Documents**: Markdown, HTML, XML, LaTeX
            - **Configurations**: JSON, YAML, TOML, INI, environment variable files
            - **Data**: CSV, TSV, log files
            - **Other text**: README, documentation comments, plain text documents

            ### Identical Core Mechanism

            Editing plain text documents uses the exact same mechanism:

            ```
            ------- SEARCH
            [text snippet from the original document]
            =======
            [replacement text content]
            +++++++ REPLACE
            ```

            ### Simple Examples

            Assume a simple README.md file:

            ```markdown
            # Project Name

            This is a test project.

            ## Feature List
            - Feature A
            - Feature B
            ```

            #### 1. Modify paragraph content

            ```
            ------- SEARCH
            This is a test project.
            =======
            This is a sample project demonstrating the SEARCH/REPLACE pattern.
            +++++++ REPLACE
            ```

            #### 2. Add new item to a list

            ```
            ------- SEARCH
            ## Feature List
            - Feature A
            - Feature B
            =======
            ## Feature List
            - Feature A
            - Feature B
            - Feature C: new incremental editing capability
            +++++++ REPLACE
            ```

            ## Mandatory Format Requirements

            1. The SEARCH block must replicate **all leading whitespace/indentation** from the original code exactly,
               including spaces before methods and statements — do not remove or modify them;
            2. The indentation of newly added code in the REPLACE block must match the surrounding code style,
               and the indentation of existing code must also be fully preserved;
            3. Never automatically strip any leading whitespace, even across multiple consecutive indented lines.

            ## Usage Recommendations (General Notes)

            1. **SEARCH should be as precise and unique as possible**: including a few extra lines of context
               reduces the risk of "matched multiple locations" errors.
            2. **Do not use ellipsis** (`...`) as a substitute for real code; the engine performs string matching.
            3. If there are indentation/blank line differences, the internal "fuzzy matching" mechanism can
               generally tolerate them, but statement order and key symbols must match.
            4. A single incremental code block may contain multiple SEARCH/REPLACE pairs,
               and the framework will execute them sequentially.
            5. For **updating/deleting contiguous code**, put it into **a single SEARCH/REPLACE pair**.
            6. The six operation modes can be combined. It is recommended to output them all at once.
            """;
    }
}
