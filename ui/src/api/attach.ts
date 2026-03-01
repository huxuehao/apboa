import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { Attach, AttachLog } from '@/types'

/**
 * 附件分页
 * GET /attach/page
 */
export function page(query: Partial<Attach> & { current?: number; page?: number; size?: number; sort?: string }) {
  return request.get<ApiResponse<PageResult<Attach>>>('/api/attach/page', {
    params: query
  })
}

/**
 * 附件日志分页
 * GET /attach/log/page
 */
export function logPage(query: Partial<AttachLog> & { current?: number; page?: number; size?: number; sort?: string }) {
  return request.get<ApiResponse<PageResult<AttachLog>>>('/api/attach/log/page', {
    params: query
  })
}

/**
 * 删除附件
 * POST /attach/delete
 */
export function remove(ids: string[]) {
  return request.post<ApiResponse<boolean>>('/api/attach/delete', ids)
}

/**
 * 根据ID获取附件
 * GET /attach/selectOne
 */
export function selectOne(id: string) {
  return request.get<ApiResponse<Attach>>('/api/attach/selectOne', {
    params: { id }
  })
}

/**
 * 根据ID列表获取附件列表
 * POST /attach/list
 */
export function list(ids: string[]) {
  return request.post<ApiResponse<Attach[]>>('/api/attach/list', ids)
}

/**
 * 上传附件
 * POST /attach/upload
 */
export function upload(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<string>>('/api/attach/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 分片串行上传
 * POST /attach/chunk-upload
 */
export function uploadChunk(formData: FormData) {
  return request.post<ApiResponse<string>>('/api/attach/chunk-upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载附件
 * GET /attach/download/{id}
 */
export function download(id: string) {
  return request.get(`/api/attach/download/${id}`, {
    responseType: 'blob'
  })
}

/**
 * 批量下载附件
 * POST /attach/batchDownload
 */
export function batchDownload(ids: string[]) {
  return request.post(`/api/attach/batchDownload`, ids, {
    responseType: 'blob'
  })
}
