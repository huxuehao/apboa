-- =====================================================
-- 长期记忆功能 - 增量迁移脚本
-- 日期: 2026-05-28
-- 描述:
--   在 agent_definition 表添加长期记忆配置字段
--   支持三种记忆类型：Mem0、ReMe（阿里通义千问）、百炼记忆库（阿里云百炼）
--   记忆数据由各记忆服务自行管理，无需建表
-- by wei.liu
-- =====================================================

-- ----------------------------
-- 1. agent_definition 表新增字段
-- ----------------------------
ALTER TABLE `agent_definition`
    ADD COLUMN `enable_long_term_memory` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用长期记忆' AFTER `enable_memory_compression`,
    ADD COLUMN `long_term_memory_config` text NULL COMMENT '长期记忆配置（JSON格式，包含memoryType/apiBaseUrl/apiKey/userId/memoryMode等）' AFTER `enable_long_term_memory`;

-- 完成
