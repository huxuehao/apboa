DROP TABLE IF EXISTS `quartz_job_info`;
CREATE TABLE `quartz_job_info` (
`id` varchar(64)  NOT NULL COMMENT '任务身份唯一标识',
`type` varchar(64)  DEFAULT NULL COMMENT '类型',
`biz_id` varchar(64)  DEFAULT NULL COMMENT '关联业务ID',
`cron` varchar(64)  DEFAULT NULL COMMENT 'cron',
`job_class` varchar(100) DEFAULT NULL COMMENT 'job类路径',
`data_map` text  COMMENT '执行参数',
`enabled` tinyint(1) DEFAULT NULL COMMENT '状态（0停止 1启动）',
PRIMARY KEY (`id`) USING BTREE
) COMMENT='quartz定时任务状态';

DROP TABLE IF EXISTS `quartz_job_log`;
CREATE TABLE `quartz_job_log` (
`id` varchar(64) NOT NULL COMMENT '定时任务日志主键',
`identity` varchar(255) DEFAULT NULL COMMENT '任务身份标识',
`start_time` datetime DEFAULT NULL COMMENT '开始时间',
`end_time` datetime DEFAULT NULL COMMENT '结束时间',
`content` text COMMENT '执行情况（1标识成功，0标识失败）',
`status` varchar(64) DEFAULT NULL COMMENT '状态',
`duration` decimal(11,0) DEFAULT NULL COMMENT '持续时间（秒）',
PRIMARY KEY (`id`)
) COMMENT='quartz定时任务日志';
