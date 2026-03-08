/**
 * 卡片操作菜单项 composable
 * 统一管理各卡片组件的操作菜单项及图标，便于后期统一修改
 *
 * @author huxuehao
 */
import { h } from 'vue'
import type { VNode } from 'vue'
import {
  EyeOutlined,
  EditOutlined,
  CheckCircleOutlined,
  StopOutlined,
  MessageOutlined,
  HistoryOutlined,
  DeleteOutlined,
  SettingOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'

/** 菜单项类型（兼容 Ant Design Menu items） */
export type CardMenuItem =
  | { key: string; label: string; icon?: () => VNode; danger?: boolean }
  | { type: 'divider' }

/**
 * 创建「查看」菜单项
 */
export function createViewItem(): CardMenuItem {
  return { key: 'view', label: '查看', icon: () => h(EyeOutlined) }
}

/**
 * 创建「编辑」菜单项
 */
export function createEditItem(): CardMenuItem {
  return { key: 'edit', label: '编辑', icon: () => h(EditOutlined) }
}

/**
 * 创建「启用/禁用」菜单项
 * @param enabled 当前是否启用
 */
export function createEnableItem(enabled: boolean): CardMenuItem {
  return {
    key: 'enable',
    label: enabled ? '禁用' : '启用',
    icon: () => h(enabled ? StopOutlined : CheckCircleOutlined),
  }
}

/**
 * 创建「删除」菜单项
 */
export function createDeleteItem(): CardMenuItem {
  return { key: 'delete', label: '删除', danger: true, icon: () => h(DeleteOutlined) }
}

/**
 * 创建「去对话」菜单项
 */
export function createGoVisitItem(): CardMenuItem {
  return { key: 'goVisit', label: '去对话', icon: () => h(MessageOutlined) }
}

/**
 * 创建「访问历史」菜单项
 */
export function createAccessLogItem(): CardMenuItem {
  return { key: 'accessLog', label: '历史对话', icon: () => h(HistoryOutlined) }
}

/**
 * 创建「配置模型」菜单项
 */
export function createConfigItem(): CardMenuItem {
  return { key: 'config', label: '配置模型', icon: () => h(SettingOutlined) }
}

/**
 * 创建「协议配置」菜单项
 */
export function createProtocolConfigItem(): CardMenuItem {
  return { key: 'protocolConfig', label: '协议配置', icon: () => h(SettingOutlined) }
}

/**
 * 创建「架构图」菜单项
 */
export function createArchitectureItem(): CardMenuItem {
  return { key: 'architecture', label: '架构图', icon: () => h(ApartmentOutlined) }
}

/**
 * 创建分隔线
 */
export function createDivider(): CardMenuItem {
  return { type: 'divider' }
}
