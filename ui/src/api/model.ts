import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { ModelConfigDTO, ModelConfigVO, ModelProviderDTO, ModelProviderVO } from '@/types'
import type { ModelConfig, ModelProvider } from '@/types'

/**
 * 模型配置分页查询
 * GET /model/config/page
 */
export function configPage(query: ModelConfigDTO) {
  return request.get<ApiResponse<PageResult<ModelConfigVO>>>('/api/model/config/page', {
    params: query
  })
}

/**
 * 模型配置详情
 * GET /model/config/{id}
 */
export function configDetail(id: string) {
  return request.get<ApiResponse<ModelConfigVO>>(`/api/model/config/${id}`)
}

/**
 * 新增模型配置
 * POST /model/config
 */
export function configSave(entity: ModelConfig) {
  return request.post<ApiResponse<boolean>>('/api/model/config', entity)
}

/**
 * 修改模型配置
 * PUT /model/config
 */
export function configUpdate(entity: ModelConfig) {
  return request.put<ApiResponse<boolean>>('/api/model/config', entity)
}

/**
 * 删除模型配置
 * DELETE /model/config
 */
export function configRemove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/model/config', { data: ids })
}

/**
 * 模型配置被哪些Agent使用
 * POST /model/config/used-with-agent
 */
export function configUsedWithAgent(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/model/config/used-with-agent', ids)
}

/**
 * 模型提供商分页查询
 * GET /model/provider/page
 */
export function providerPage(query: ModelProviderDTO) {
  return request.get<ApiResponse<PageResult<ModelProviderVO>>>('/api/model/provider/page', {
    params: query
  })
}

/**
 * 模型提供商详情
 * GET /model/provider/{id}
 */
export function providerDetail(id: string) {
  return request.get<ApiResponse<ModelProviderVO>>(`/api/model/provider/${id}`)
}

/**
 * 新增模型提供商
 * POST /model/provider
 */
export function providerSave(entity: ModelProvider) {
  return request.post<ApiResponse<boolean>>('/api/model/provider', entity)
}

/**
 * 修改模型提供商
 * PUT /model/provider
 */
export function providerUpdate(entity: ModelProvider) {
  return request.put<ApiResponse<boolean>>('/api/model/provider', entity)
}

/**
 * 删除模型提供商
 * DELETE /model/provider
 */
export function providerRemove(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/model/provider', { data: ids })
}

/**
 * 模型提供商被哪些模型使用
 * POST /model/provider/used-with-model
 */
export function providerUsedWithModel(ids: string[]) {
  return request.post<ApiResponse<unknown[]>>('/api/model/provider/used-with-model', ids)
}
