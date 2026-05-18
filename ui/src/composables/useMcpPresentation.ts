import type { McpServerVO } from '@/types'
import { McpActivationStatus } from '@/types'

type McpPresentationTarget = Pick<McpServerVO, 'enabled' | 'activationStatus' | 'toolCount' | 'availableToolCount'>

export interface McpPrimaryAction {
  key: 'activate' | 'sync'
  label: string
}

export function getMcpConnectionStatusText(mcp: McpPresentationTarget): string {
  if (!mcp.enabled) {
    return '已停用'
  }

  if (mcp.activationStatus === McpActivationStatus.ACTIVE && (mcp.toolCount || 0) === 0) {
    return '已连接（无工具）'
  }

  switch (mcp.activationStatus) {
    case McpActivationStatus.ACTIVE:
      return '可用'
    case McpActivationStatus.ACTIVATING:
      return '连接中'
    case McpActivationStatus.FAILED:
      return '连接失败'
    default:
      return '待连接'
  }
}

export function getMcpConnectionStatusColor(mcp: McpPresentationTarget): string {
  if (!mcp.enabled) {
    return 'default'
  }

  if (mcp.activationStatus === McpActivationStatus.ACTIVE && (mcp.toolCount || 0) === 0) {
    return 'gold'
  }

  switch (mcp.activationStatus) {
    case McpActivationStatus.ACTIVE:
      return 'success'
    case McpActivationStatus.ACTIVATING:
      return 'processing'
    case McpActivationStatus.FAILED:
      return 'error'
    default:
      return 'default'
  }
}

export function getMcpPrimaryAction(mcp: McpPresentationTarget): McpPrimaryAction | null {
  if (!mcp.enabled || mcp.activationStatus === McpActivationStatus.ACTIVATING) {
    return null
  }

  if (mcp.activationStatus === McpActivationStatus.ACTIVE) {
    return { key: 'sync', label: '刷新工具' }
  }

  if (mcp.activationStatus === McpActivationStatus.FAILED) {
    return { key: 'activate', label: '重试连接' }
  }

  return { key: 'activate', label: '连接' }
}

export function getMcpUnavailableReason(mcp: McpPresentationTarget): string {
  if (!mcp.enabled) {
    return '已停用'
  }
  if (mcp.activationStatus === McpActivationStatus.ACTIVATING) {
    return '连接中'
  }
  if (mcp.activationStatus === McpActivationStatus.FAILED) {
    return '连接失败'
  }
  if (mcp.activationStatus !== McpActivationStatus.ACTIVE) {
    return '待连接'
  }
  if ((mcp.availableToolCount || 0) <= 0 && (mcp.toolCount || 0) > 0) {
    return '无全局可用工具'
  }
  if ((mcp.availableToolCount || 0) <= 0) {
    return '已连接（无工具）'
  }
  return ''
}
