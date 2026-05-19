/**
 * 计划状态追踪 composable
 *
 * 通过捕获 PlanNotebook 的工具调用来实时维护当前计划的状态，
 * 供 PlanPanel 组件渲染使用。
 *
 * @author huxuehao
 */
import { ref, computed } from 'vue'
import type { PlanInfo, SubTaskInfo, SubTaskDisplayState } from '@/types'

/** 涉及计划状态变更的工具名称集合 */
const PLAN_MUTATION_TOOLS = new Set([
  'create_plan',
  'update_plan_info',
  'revise_current_plan',
  'update_subtask_state',
  'finish_subtask',
  'finish_plan',
  'recover_historical_plan'
])

/**
 * 安全解析 JSON，失败时返回 null
 */
function safeJsonParse(raw: string): Record<string, unknown> | null {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/**
 * 将工具参数中的子任务 Map 数组转换为 SubTaskInfo 数组
 */
function mapToSubtaskList(
  subtaskMaps: Array<Record<string, unknown>>
): SubTaskInfo[] {
  return subtaskMaps.map((m) => ({
    name: (m.name as string) || 'Unnamed Subtask',
    description: (m.description as string) || '',
    expectedOutcome: (m.expected_outcome as string) || '',
    state: 'todo' as SubTaskDisplayState
  }))
}

export function usePlanTracking() {
  const currentPlan = ref<PlanInfo | null>(null)

  /** toolCallId -> toolCallName 映射 */
  const toolNameMap = new Map<string, string>()

  /** toolCallId -> 累积的 args JSON 字符串 */
  const toolArgsMap = new Map<string, string>()

  /** 是否有活跃计划 */
  const hasPlan = computed(() => currentPlan.value !== null)

  /** 计划中子任务总数 */
  const subtaskCount = computed(() => currentPlan.value?.subtasks.length ?? 0)

  /** 计划是否已终结（done 或 abandoned） */
  const isPlanFinished = ref(false)

  /**
   * 工具调用开始时记录 toolCallId 与名称的映射
   */
  function onToolStart(toolCallId: string, toolCallName: string): void {
    if (PLAN_MUTATION_TOOLS.has(toolCallName)) {
      toolNameMap.set(toolCallId, toolCallName)
      toolArgsMap.set(toolCallId, '')
    }
  }

  /**
   * 工具调用参数增量累积（partialArgs 是累计值，直接覆盖即可）
   */
  function onToolArgs(toolCallId: string, partialArgs: string): void {
    if (toolArgsMap.has(toolCallId)) {
      toolArgsMap.set(toolCallId, partialArgs)
    }
  }

  /**
   * 工具调用结果到达时，根据累积的参数更新计划状态
   */
  function onToolResult(toolCallId: string): void {
    const toolName = toolNameMap.get(toolCallId)
    const rawArgs = toolArgsMap.get(toolCallId)

    // 清理映射
    toolNameMap.delete(toolCallId)
    toolArgsMap.delete(toolCallId)

    if (!toolName || !rawArgs) return

    const args = safeJsonParse(rawArgs)
    if (!args) return

    processPlanMutation(toolName, args)
  }

  /**
   * 根据工具名和参数处理计划状态变更
   */
  function processPlanMutation(
    toolName: string,
    args: Record<string, unknown>
  ): void {
    switch (toolName) {
      case 'create_plan':
        handleCreatePlan(args)
        break
      case 'update_plan_info':
        handleUpdatePlanInfo(args)
        break
      case 'revise_current_plan':
        handleReviseCurrentPlan(args)
        break
      case 'update_subtask_state':
        handleUpdateSubtaskState(args)
        break
      case 'finish_subtask':
        handleFinishSubtask(args)
        break
      case 'finish_plan':
        handleFinishPlan(args)
        break
      case 'recover_historical_plan':
        handleRecoverHistoricalPlan(args)
        break
    }
  }

  /**
   * 创建新计划
   */
  function handleCreatePlan(args: Record<string, unknown>): void {
    const name = (args.name as string) || 'Unnamed Plan'
    const description = (args.description as string) || ''
    const expectedOutcome = (args.expected_outcome as string) || ''
    const subtaskMaps =
      (args.subtasks as Array<Record<string, unknown>>) || []

    currentPlan.value = {
      name,
      description,
      expectedOutcome,
      subtasks: mapToSubtaskList(subtaskMaps)
    }
    isPlanFinished.value = false
  }

  /**
   * 更新计划信息
   */
  function handleUpdatePlanInfo(args: Record<string, unknown>): void {
    if (!currentPlan.value) return

    const name = args.name as string | undefined
    const description = args.description as string | undefined
    const expectedOutcome = args.expected_outcome as string | undefined

    if (name && name.trim()) {
      currentPlan.value.name = name.trim()
    }
    if (description && description.trim()) {
      currentPlan.value.description = description.trim()
    }
    if (expectedOutcome && expectedOutcome.trim()) {
      currentPlan.value.expectedOutcome = expectedOutcome.trim()
    }
  }

  /**
   * 修订当前计划（添加/修改/删除子任务）
   */
  function handleReviseCurrentPlan(args: Record<string, unknown>): void {
    if (!currentPlan.value) return

    const subtaskIdx = args.subtask_idx as number
    const action = args.action as string
    const subtaskMap = args.subtask as Record<string, unknown> | undefined
    const subtasks = currentPlan.value.subtasks

    switch (action) {
      case 'add': {
        if (subtaskIdx < 0 || subtaskIdx > subtasks.length) return
        if (!subtaskMap) return
        const newSubtask: SubTaskInfo = {
          name: (subtaskMap.name as string) || 'Unnamed Subtask',
          description: (subtaskMap.description as string) || '',
          expectedOutcome: (subtaskMap.expected_outcome as string) || '',
          state: 'todo'
        }
        subtasks.splice(subtaskIdx, 0, newSubtask)
        break
      }
      case 'revise': {
        if (subtaskIdx < 0 || subtaskIdx >= subtasks.length) return
        if (!subtaskMap) return
        const existing = subtasks[subtaskIdx]!
        subtasks[subtaskIdx] = {
          name: (subtaskMap.name as string) || existing.name,
          description:
            (subtaskMap.description as string) || existing.description,
          expectedOutcome:
            (subtaskMap.expected_outcome as string) ||
            existing.expectedOutcome,
          state: existing.state
        }
        break
      }
      case 'delete': {
        if (subtaskIdx < 0 || subtaskIdx >= subtasks.length) return
        subtasks[subtaskIdx]!.state = 'removed'
        break
      }
    }
  }

  /**
   * 更新子任务状态
   */
  function handleUpdateSubtaskState(args: Record<string, unknown>): void {
    if (!currentPlan.value) return

    const subtaskIdx = args.subtask_idx as number
    const state = args.state as string
    const subtasks = currentPlan.value.subtasks

    if (subtaskIdx < 0 || subtaskIdx >= subtasks.length) return
    if (state === 'in_progress' || state === 'abandoned') {
      subtasks[subtaskIdx]!.state = state
    }
  }

  /**
   * 完成子任务
   */
  function handleFinishSubtask(args: Record<string, unknown>): void {
    if (!currentPlan.value) return

    const subtaskIdx = args.subtask_idx as number
    const outcome = args.subtask_outcome as string | undefined
    const subtasks = currentPlan.value.subtasks

    if (subtaskIdx < 0 || subtaskIdx >= subtasks.length) return

    subtasks[subtaskIdx]!.state = 'done'
    if (outcome) {
      subtasks[subtaskIdx]!.outcome = outcome
    }

    // 自动激活下一个子任务（如果存在）
    if (subtaskIdx + 1 < subtasks.length) {
      const next = subtasks[subtaskIdx + 1]!
      if (next.state === 'todo') {
        next.state = 'in_progress'
      }
    }
  }

  /**
   * 完成/放弃计划
   */
  function handleFinishPlan(args: Record<string, unknown>): void {
    const state = args.state as string | undefined
    isPlanFinished.value = true

    // 当计划被放弃时，将剩余非终态子任务也标记为 abandoned
    if (state === 'abandoned' && currentPlan.value) {
      currentPlan.value.subtasks.forEach((t) => {
        if (t.state !== 'done' && t.state !== 'removed' && t.state !== 'abandoned') {
          t.state = 'abandoned'
        }
      })
    }
  }

  /**
   * 恢复历史计划（参数中无完整数据，仅做标记重置）
   */
  function handleRecoverHistoricalPlan(_args: Record<string, unknown>): void {
    // 历史计划的完整数据不在工具参数中，此处仅重置完成标记
    // 后续可通过 planActive / create_plan 获取完整数据
    isPlanFinished.value = false
  }

  /**
   * 重置计划状态（中止对话时调用）
   */
  function resetPlan(): void {
    currentPlan.value = null
    isPlanFinished.value = false
    toolNameMap.clear()
    toolArgsMap.clear()
  }

  return {
    currentPlan,
    hasPlan,
    subtaskCount,
    isPlanFinished,
    onToolStart,
    onToolArgs,
    onToolResult,
    resetPlan
  }
}
