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

