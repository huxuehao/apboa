import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { SkillPackageDTO, SkillPackageVO } from '@/types'
import type { SkillPackage } from '@/types'

/**
 * 分页查询
 * GET /skill/page
 */
export function page(query: SkillPackageDTO) {
  return request.get<ApiResponse<PageResult<SkillPackageVO>>>('/api/skill/page', {
    params: query
  })
}

/**
 * 详情
 * GET /skill/{id}
 */
export function detail(id: string) {
  return request.get<ApiResponse<SkillPackageVO>>(`/api/skill/${id}`)
}

/**
 * 新增
 * POST /skill
 */
export function save(entity: SkillPackage) {
  return request.post<ApiResponse<boolean>>('/api/skill', entity)
}

/**
 * 修改
 * PUT /skill
 */
export function update(entity: SkillPackage) {
  return request.put<ApiResponse<boolean>>('/api/skill', entity)
}

/**
 * 删除
 * DELETE /skill
 */
export function remove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/skill', { data: ids })
}

/**
 * 被哪些Agent使用
 * POST /skill/used-with-agent
 */
export function usedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/skill/used-with-agent', ids)
}

/**
 * 获取所有分类
 * GET /api/skill/get/categories
 */
export function listCategories() {
  return request.get<ApiResponse<string[]>>('/api/skill/get/categories')
}
