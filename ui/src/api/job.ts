import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { JobInfo } from '@/types'

/**
 * 查询所有定时任务
 * GET /job/list
 */
export function list() {
  return request.get<ApiResponse<JobInfo[]>>('/api/job/list')
}

/**
 * 新增定时任务
 * POST /job/add
 */
export function add(jobInfo: JobInfo) {
  return request.post<ApiResponse<boolean>>('/api/job/add', jobInfo)
}

/**
 * 修改定时任务
 * POST /job/update
 */
export function update(jobInfo: JobInfo) {
  return request.post<ApiResponse<boolean>>('/api/job/update', jobInfo)
}

/**
 * 修改定时任务的 cron 表达式
 * GET /job/updateCron
 */
export function updateCron(id: string, cron: string) {
  return request.get<ApiResponse<boolean>>('/api/job/updateCron', {
    params: { id, cron }
  })
}

/**
 * 删除定时任务
 * GET /job/delete
 */
export function remove(id: string) {
  return request.get<ApiResponse<boolean>>('/api/job/delete', {
    params: { id }
  })
}

/**
 * 启动定时任务
 * GET /job/start
 */
export function start(id: string) {
  return request.get<ApiResponse<boolean>>('/api/job/start', {
    params: { id }
  })
}

/**
 * 停止定时任务
 * GET /job/stop
 */
export function stop(id: string) {
  return request.get<ApiResponse<boolean>>('/api/job/stop', {
    params: { id }
  })
}

/**
 * 根据业务ID查询定时任务
 * GET /job/getByBizId
 */
export function getByBizId(bizId: string) {
  return request.get<ApiResponse<JobInfo>>('/api/job/getByBizId', {
    params: { bizId }
  })
}

/**
 * 根据业务ID删除定时任务（解绑）
 * GET /job/deleteByBizId
 */
export function deleteByBizId(bizId: string) {
  return request.get<ApiResponse<boolean>>('/api/job/deleteByBizId', {
    params: { bizId }
  })
}
