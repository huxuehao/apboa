import request from '@/utils/request'
import type { ApiResponse } from '@/types'
import type { RagDocument, RagDocumentChunk } from '@/types'

/**
 * 上传文档到知识库
 */
export function uploadDocument(file: File, knowledgeBaseConfigId: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('knowledgeBaseConfigId', knowledgeBaseConfigId)
  return request.post<ApiResponse<number>>('/api/rag/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 查询知识库下的文档列表
 */
export function listDocuments(knowledgeBaseConfigId: string) {
  return request.get<ApiResponse<RagDocument[]>>('/api/rag/document/list', {
    params: { knowledgeBaseConfigId }
  })
}

/**
 * 删除文档
 */
export function deleteDocuments(ids: string[]) {
  return request.delete<ApiResponse<boolean>>('/api/rag/document', { data: ids })
}

/**
 * 查询文档分块列表
 */
export function listChunks(documentId: string) {
  return request.get<ApiResponse<RagDocumentChunk[]>>('/api/rag/document/chunks', {
    params: { documentId }
  })
}

/**
 * RAG检索测试
 */
export function search(params: { knowledgeBaseConfigId: string; query: string; limit?: number; scoreThreshold?: number }) {
  return request.post<ApiResponse<Record<string, unknown>[]>>('/api/rag/document/search', params)
}

/**
 * 下载文档原始文件
 */
export function downloadDocument(documentId: string) {
  return request.get(`/api/rag/document/download/${documentId}`, {
    responseType: 'blob'
  })
}

/**
 * 重新上传文档（替换原有文件并重新处理）
 */
export function reUploadDocument(documentId: string, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<boolean>>(`/api/rag/document/re-upload/${documentId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 重新分块处理文档
 */
export function reChunkDocument(documentId: string) {
  return request.post<ApiResponse<boolean>>(`/api/rag/document/re-chunk/${documentId}`)
}
