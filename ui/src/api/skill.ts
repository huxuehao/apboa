import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types'
import type { SkillPackageDTO, SkillPackageVO, LocalImportConfig, GitImportConfig, SkillImportResult } from '@/types'
import type { SkillPackage } from '@/types'
import type { SkillFileTreeNode } from '@/types'

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
  return request.post<ApiResponse<number>>('/api/skill', entity)
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

/**
 * 从本地导入
 * POST /skill/import/local
 */
export function importFromLocal(config: LocalImportConfig) {
  return request.post<ApiResponse<SkillImportResult>>('/api/skill/import/local', config)
}

/**
 * 从Git导入
 * POST /skill/import/git
 */
export function importFromGit(config: GitImportConfig) {
  return request.post<ApiResponse<SkillImportResult>>('/api/skill/import/git', config)
}

/**
 * 从压缩包上传导入
 * POST /skill/import/upload
 */
export function importFromUpload(file: File, category: string, cover: boolean) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  formData.append('cover', String(cover))
  return request.post<ApiResponse<SkillImportResult>>('/api/skill/import/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 文件操作 API ====================

/**
 * 获取技能包文件树
 * GET /skill/{skillId}/tree
 */
export function getTree(skillId: string) {
  return request.get<ApiResponse<SkillFileTreeNode[]>>(`/api/skill/${skillId}/tree`)
}

/**
 * 创建文件
 * POST /skill/{skillId}/files
 */
export function createFile(skillId: string, data: { parentPath: string; fileName: string; content?: string }) {
  return request.post<ApiResponse<SkillFileTreeNode>>(`/api/skill/${skillId}/files`, data)
}

/**
 * 更新入库文件内容
 * PUT /skill/files/{fileId}
 */
export function updateFile(fileId: string, content: string) {
  return request.put<ApiResponse<boolean>>(`/api/skill/files/${fileId}`, { content })
}

/**
 * 更新纯文件系统文件
 * PUT /skill/{skillId}/filesystem-write
 */
export function writeFileSystemFile(skillId: string, data: { path: string; content: string }) {
  return request.put<ApiResponse<boolean>>(`/api/skill/${skillId}/filesystem-write`, data)
}

/**
 * 删除入库文件
 * DELETE /skill/files/{fileId}
 */
export function deleteDbFile(fileId: string) {
  return request.delete<ApiResponse<boolean>>(`/api/skill/files/${fileId}`)
}

/**
 * 删除纯文件系统节点（文件或目录）
 * DELETE /skill/{skillId}/filesystem
 */
export function deleteFileSystemNode(skillId: string, data: { path: string; directory: boolean }) {
  return request.delete<ApiResponse<boolean>>(`/api/skill/${skillId}/filesystem`, { data })
}

/**
 * 创建文件夹
 * POST /skill/{skillId}/directories
 */
export function createDirectory(skillId: string, data: { parentPath: string; dirName: string }) {
  return request.post<ApiResponse<boolean>>(`/api/skill/${skillId}/directories`, data)
}

/**
 * 上传文件
 * POST /skill/{skillId}/upload
 */
export function uploadFile(skillId: string, parentPath: string, file: File) {
  const formData = new FormData()
  formData.append('parentPath', parentPath)
  formData.append('file', file)
  return request.post<ApiResponse<SkillFileTreeNode>>(`/api/skill/${skillId}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 读取纯文件系统文件内容
 * GET /skill/{skillId}/file-content
 */
export function getFileContent(skillId: string, path: string) {
  return request.get<ApiResponse<string>>(`/api/skill/${skillId}/file-content`, {
    params: { path }
  })
}

/**
 * 获取允许的文件扩展名白名单
 * GET /skill/allowed-extensions
 */
export function getAllowedExtensions() {
  return request.get<ApiResponse<string[]>>('/api/skill/allowed-extensions')
}

/**
 * 按路径从文件系统下载文件
 * GET /skill/{skillId}/download
 */
export function downloadFile(skillId: string, path: string) {
  return request.get(`/api/skill/${skillId}/download`, {
    params: { path },
    responseType: 'blob',
  })
}

/**
 * 下载整个技能包为压缩包
 * GET /skill/{skillId}/download-zip
 */
export function downloadZip(skillId: string) {
  return request.get(`/api/skill/${skillId}/download-zip`, {
    responseType: 'blob',
  })
}
