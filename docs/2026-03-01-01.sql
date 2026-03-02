INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2020017365391609857, 'get_current_datetime', 'get_current_datetime', '获取当前的日期时间', '通用', 'BUILTIN', '[{\"name\":\"format\",\"description\":\"日期时间格式，默认值 yyyy-MM-dd HH:mm:ss\",\"type\":\"string\",\"defaultValue\":null,\"required\":false}]', NULL, 'com.hxh.apboa.core.tool.builtins.GetCurrentTimeTool', NULL, NULL, 0, 1, '1.0.0', '2026-02-07 14:10:45', '2026-03-02 14:34:27', NULL, 0);
INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2021603578967851010, '获取当前用户的个人信息', 'get_current_userinfo', '通过此工具，你可以获取到当前和你对话用户的个人信息，包括：姓名、年龄、性别和爱好。\n', '通用', 'CUSTOM', '[]', NULL, NULL, 'JAVA', 'import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;\nimport java.util.*;\n\npublic class Demo implements IDynamicAgentTool {\n\n    @Override\n    public Object execute(Object... args) {\n        Map<String, Object> resMap = new HashMap<String, Object>() {{\n            put(\"name\", \"寒江\");\n            put(\"age\", 28);\n            put(\"gender\", \"男\");\n            put(\"hobby\", \"跑步、乒乓球、编程\");\n        }};\n      \n        return resMap;\n    }\n}', 1, 1, '1.0.0', '2026-02-11 23:13:47', '2026-02-17 16:20:47', 1111111111111111111, 1111111111111111111);
INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2025895208227008514, '数学计算函数', 'math_calculator', '通用数学计算工具，支持加、减、乘、除、取余、幂运算等基本数学运算，接收两个数字字符串和一个运算符，返回计算结果及详细运算信息。', '通用', 'CUSTOM', '[{\"name\":\"num1\",\"description\":\"第一个数字\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true},{\"name\":\"num2\",\"description\":\"第二个数字\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true},{\"name\":\"operator\",\"description\":\"运算符，只支持 \\\"+\\\", \\\"-\\\", \\\"*\\\", \\\"/\\\", \\\"%\\\", \\\"pow\\\"\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true}]', NULL, NULL, 'JAVA', 'import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;\nimport java.util.*;\n\npublic class MathCalculator implements IDynamicAgentTool {\n\n    @Override\n    public Object execute(Object... args) {\n        // 参数验证\n        if (args == null || args.length < 3) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"参数不足，需要3个参数：数字1、数字2、运算符\");\n            errorMap.put(\"required_format\", \"第一个参数: 数字1(字符串), 第二个参数: 数字2(字符串), 第三个参数: 运算符(字符串)\");\n            return errorMap;\n        }\n\n        try {\n            // 获取参数（都是字符串类型）\n            String num1Str = args[0].toString();\n            String num2Str = args[1].toString();\n            String operator = args[2].toString();\n\n            // 转换为数字\n            double num1 = Double.parseDouble(num1Str);\n            double num2 = Double.parseDouble(num2Str);\n\n            // 计算结果\n            double result = 0;\n            String calculation = \"\";\n\n            switch (operator) {\n                case \"+\":\n                    result = num1 + num2;\n                    calculation = num1 + \" + \" + num2 + \" = \" + result;\n                    break;\n                case \"-\":\n                    result = num1 - num2;\n                    calculation = num1 + \" - \" + num2 + \" = \" + result;\n                    break;\n                case \"*\":\n                    result = num1 * num2;\n                    calculation = num1 + \" × \" + num2 + \" = \" + result;\n                    break;\n                case \"/\":\n                    if (num2 == 0) {\n                        throw new ArithmeticException(\"除数不能为0\");\n                    }\n                    result = num1 / num2;\n                    calculation = num1 + \" ÷ \" + num2 + \" = \" + result;\n                    break;\n                case \"%\":\n                    result = num1 % num2;\n                    calculation = num1 + \" % \" + num2 + \" = \" + result;\n                    break;\n                case \"pow\":\n                    result = Math.pow(num1, num2);\n                    calculation = num1 + \" ^ \" + num2 + \" = \" + result;\n                    break;\n                default:\n                    Map<String, Object> errorMap = new HashMap<>();\n                    errorMap.put(\"error\", \"不支持的运算符: \" + operator);\n                    errorMap.put(\"supported_operators\", \"+, -, *, /, %, pow\");\n                    return errorMap;\n            }\n\n            // 构建返回结果\n            Map<String, Object> resMap = new HashMap<String, Object>() {{\n                put(\"num1\", num1Str);\n                put(\"num2\", num2Str);\n                put(\"operator\", operator);\n                put(\"result\", result);\n                put(\"calculation\", calculation);\n                put(\"timestamp\", new Date().toString());\n            }};\n\n            return resMap;\n\n        } catch (NumberFormatException e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"数字格式错误，请确保传入有效的数字字符串\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        } catch (ArithmeticException e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"数学运算错误\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        } catch (Exception e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"未知错误\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        }\n    }\n}', 0, 1, '1.0.0', '2026-02-23 19:27:12', '2026-02-23 19:27:12', 1111111111111111111, 1111111111111111111);

INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024821058360176641, 'lbs-amap-http-mcp', 'HTTP', 1, 'SYNC', 30000, '{\"url\":\"https://mcp.amap.com/mcp\",\"queryParams\":[{\"key\":\"key\",\"value\":\"yourself key\"}],\"headers\":[]}', '高度地图 HTTP MCP', 'UNKNOWN', NULL, '2026-02-20 20:18:54', '2026-02-20 23:52:37', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024825210448453633, 'lbs-amap-sse-mcp', 'SSE', 1, 'SYNC', 30000, '{\"url\":\"https://mcp.amap.com/sse\",\"queryParams\":[{\"key\":\"key\",\"value\":\"yourself key\"}],\"headers\":[]}', '高度地图 SSE MCP', 'UNKNOWN', NULL, '2026-02-20 20:35:24', '2026-02-20 23:52:51', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024829036727439361, 'daidu-map-http-mcp', 'HTTP', 0, 'SYNC', 30000, '{\"url\":\"https://mcp.map.baidu.com/mcp\",\"queryParams\":[{\"key\":\"ak\",\"value\":\"yourself key\"}],\"headers\":[]}', '百度地图 HTTP MCP', 'UNKNOWN', NULL, '2026-02-20 20:50:37', '2026-02-22 00:11:27', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024829212489748481, 'baidu-amp-sse-mcp', 'SSE', 0, 'SYNC', 30000, '{\"url\":\"https://mcp.map.baidu.com/sse\",\"queryParams\":[{\"key\":\"ak\",\"value\":\"yourself key\"}],\"headers\":[]}', '百度地图 SSE MCP', 'UNKNOWN', NULL, '2026-02-20 20:51:18', '2026-02-22 00:11:32', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024830349192269825, 'baidu-maps', 'STDIO', 1, 'SYNC', 30000, '{\"command\":\"D:\\\\\\\\environment\\\\\\\\nvm\\\\\\\\nodejs\\\\\\\\npx.cmd\",\"args\":[\"-y\",\"@baidumap/mcp-server-baidu-map\"],\"env\":[{\"key\":\"BAIDU_MAP_API_KEY\",\"value\":\"yourself key\"}],\"encoding\":\"UTF-8\"}', '百度地图 STDIO MCP', 'UNKNOWN', NULL, '2026-02-20 20:55:49', '2026-02-20 21:02:07', 1111111111111111111, 1111111111111111111);

DROP TABLE IF EXISTS `storage_protocol`;
CREATE TABLE `storage_protocol` (
`id` bigint NOT NULL COMMENT '主键',
`name` varchar(255) DEFAULT NULL COMMENT '名称',
`protocol` varchar(255) DEFAULT NULL COMMENT '存储协议',
`protocol_config` varchar(1000) DEFAULT NULL COMMENT '协议配置',
`create_by` varchar(40) DEFAULT NULL COMMENT '创建人',
`create_at` datetime DEFAULT NULL COMMENT '创建时间',
`update_by` varchar(40) DEFAULT NULL COMMENT '修改人',
`update_at` datetime DEFAULT NULL COMMENT '修改时间',
`remark` varchar(255) DEFAULT NULL COMMENT '备注',
`valid` int DEFAULT '1' COMMENT '是否有效',
PRIMARY KEY (`id`)
) COMMENT='文件存储协议配置';
CREATE INDEX idx_protocol_protocol ON storage_protocol(`protocol`);
CREATE INDEX idx_protocol_valid ON storage_protocol(`valid`);

DROP TABLE IF EXISTS `attach`;
CREATE TABLE `attach` (
`id` bigint NOT NULL COMMENT '主键',
`file_id` bigint DEFAULT NULL COMMENT '文件id',
`link` varchar(1000) DEFAULT NULL COMMENT '附件地址',
`domain` varchar(500) DEFAULT NULL COMMENT '附件域名',
`name` varchar(500) DEFAULT NULL COMMENT '附件名称',
`original_name` varchar(500) DEFAULT NULL COMMENT '附件原名',
`extension` varchar(12) DEFAULT NULL COMMENT '附件拓展名',
`attach_size` bigint DEFAULT NULL COMMENT '附件大小',
`path` varchar(255) DEFAULT NULL COMMENT '存储路径',
`create_by` bigint DEFAULT NULL COMMENT '创建人',
`create_at` datetime DEFAULT NULL COMMENT '创建时间',
`update_by` bigint DEFAULT NULL COMMENT '修改人',
`update_at` datetime DEFAULT NULL COMMENT '修改时间',
`protocol` varchar(40) DEFAULT NULL COMMENT '存储协议',
`status` int DEFAULT NULL COMMENT '状态',
PRIMARY KEY (`id`)
) COMMENT='附件表';
CREATE INDEX idx_attach_file_id ON attach(`file_id`);
CREATE INDEX idx_attach_name ON attach(`name`);
CREATE INDEX idx_attach_path ON attach(`path`);
CREATE INDEX idx_attach_protocol_status ON attach(`protocol`, `status`);
CREATE INDEX idx_attach_create_at ON attach(`create_at`);
CREATE INDEX idx_attach_protocol_create ON attach(`protocol`, `create_at`);
CREATE INDEX idx_attach_extension ON attach(`extension`);

DROP TABLE IF EXISTS `attach_log`;
CREATE TABLE `attach_log` (
`id` bigint NOT NULL COMMENT '主键',
`file_id` bigint DEFAULT NULL COMMENT '文件id',
`original_name` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '附件原名',
`extension` varchar(12) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '附件拓展名',
`attach_size` bigint DEFAULT NULL COMMENT '附件大小',
`opt_user` bigint DEFAULT NULL COMMENT '操作人',
`opt_user_name` varchar(40) DEFAULT NULL COMMENT '操作人名称',
`opt_time` datetime DEFAULT NULL COMMENT '操作时间',
`opt_ip` varchar(20) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '操作IP',
`opt_type` varchar(10) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '操作类型',
PRIMARY KEY (`id`)
) COMMENT='附件操作日志表';
CREATE INDEX idx_log_file_id ON attach_log(`file_id`);
CREATE INDEX idx_log_opt_time ON attach_log(`opt_time`);
CREATE INDEX idx_log_time_type ON attach_log(`opt_time`, `opt_type`);

DROP TABLE IF EXISTS `attach_chunk`;
CREATE TABLE `attach_chunk` (
`id` bigint NOT NULL COMMENT '主键',
`chunk_hash` varchar(40) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '分片的hash值',
`chunk_index` int DEFAULT NULL COMMENT '分片的索引',
`chunk_totals` int DEFAULT NULL COMMENT '分片总数',
`file_key` varchar(40) DEFAULT NULL COMMENT '文件唯一标识',
`file_total_size` int DEFAULT NULL COMMENT '文件大小',
`file_name` varchar(255) DEFAULT NULL COMMENT '文件名称',
`create_by` bigint DEFAULT NULL COMMENT '创建人',
`create_at` datetime DEFAULT NULL COMMENT '创建时间',
PRIMARY KEY (`id`)
) COMMENT='附件表分片记录表';
CREATE INDEX idx_chunk_file_key ON attach_chunk(`file_key`);
CREATE INDEX idx_chunk_create_at ON attach_chunk(`create_at`);

DROP TABLE IF EXISTS `params`;
CREATE TABLE `params` (
`id` bigint NOT NULL COMMENT '主键',
`param_name` varchar(100) DEFAULT NULL COMMENT '参数名称',
`param_key` varchar(100) DEFAULT NULL COMMENT '参数Key',
`param_value` varchar(300) DEFAULT NULL COMMENT '参数Value',
PRIMARY KEY (`id`)
) COMMENT='参数表';
CREATE INDEX idx_param_key ON `params` (`param_key`);
CREATE INDEX idx_param_name ON `params` (`param_name`);
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (1, '访问Token有效期（单位 ms）', 'ACCESS_TOKEN_TTL', '21600000');
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (2, '刷新Token有效期（单位 ms）', 'REFRESH_TOKEN_TTL', '64800000');
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (3, '单个文件大小限制（单位 MB）', 'SINGLE_FILE_MAX_SIZE', '5');
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (4, '支持的图片文件类型', 'ALLOW_IMAGE_FILE_TYPE', 'png,jpeg,png,gif,webp');
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (5, '支持的音频文件类型', 'ALLOW_AUDIO_FILE_TYPE', 'mp3,wav,mpeg');
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`) VALUES (6, '支持的视频文件类型', 'ALLOW_VIDEO_FILE_TYPE', 'mp4,mpeg');

