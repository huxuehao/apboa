/**
 * PlanNotebook 计划相关类型定义
 *
 * @author huxuehao
 */

/** 子任务状态 */
export type SubTaskState = 'todo' | 'in_progress' | 'done' | 'abandoned';

/** 子任务附加展示状态（前端专属，用于保留已删除子任务的展示） */
export type SubTaskDisplayState = SubTaskState | 'removed';

/** 子任务信息 */
export interface SubTaskInfo {
  name: string;
  description: string;
  expectedOutcome: string;
  state: SubTaskDisplayState;
  outcome?: string;
}

/** 计划信息 */
export interface PlanInfo {
  name: string;
  description: string;
  expectedOutcome: string;
  subtasks: SubTaskInfo[];
}
