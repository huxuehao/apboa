ALTER TABLE `agent_definition`
    ADD COLUMN `show_tool_process` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否显示工具调用过程' AFTER `enable_planning`;
