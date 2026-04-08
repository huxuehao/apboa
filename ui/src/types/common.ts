/**
 * 通用类型定义
 *
 * @author huxuehao
 */

/**
 * 基础实体类型
 */
export interface BaseEntity {
  id?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
}

/**
 * 分页参数
 */
export interface PageParams {
  page?: number
  size?: number
  sort?: string
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * API响应结果
 */
export interface ApiResponse<T = unknown> {
  code: number
  success: boolean
  data: T
  msg: string
}
