import { http } from '@/utils/http'

// 知识库相关类型定义
export interface KnowledgeBase {
  id: string
  uuid: string
  name: string
  description?: string
  icon?: string
  scopeType: string
  scopeId?: number
  parentKbId?: string
  ownerId: string
  accessLevel: string
  documentCount: number
  chunkCount: number
  totalSize: number
  chunkSize: number
  chunkOverlap: number
  embeddingModel?: string
  embeddingModelId?: string
  retrievalConfig?: {
    top_k?: number
    similarity_threshold?: number
    max_context_length?: number
  }
  createTime: string
  updateTime: string
}

export interface KnowledgeBaseCreateDTO {
  name: string
  description?: string
  icon?: string
  scopeType: string
  scopeId?: number
  parentKbId?: string
  accessLevel?: string
  chunkSize?: number
  chunkOverlap?: number
  embeddingModelId?: string
  retrievalConfig?: {
    top_k?: number
    similarity_threshold?: number
    max_context_length?: number
  }
}

export interface KnowledgeBasePatchDTO {
  name?: string
  description?: string
  icon?: string
  accessLevel?: string
  chunkSize?: number
  chunkOverlap?: number
  embeddingModelId?: string
  retrievalConfig?: {
    top_k?: number
    similarity_threshold?: number
    max_context_length?: number
  }
}

export interface Document {
  uuid: string
  name: string
  filename: string
  fileUrl?: string
  fileSize: number
  fileType: string
  chunkCount?: number
  status: 'uploading' | 'processing' | 'processed' | 'failed'
  errorMessage?: string
  processedAt?: string
  knowledgeBaseId: string
  userId: string
  createdAt: string
  updatedAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export interface RagQueryResultItem {
  vector_id: number
  chunk_index: number
  score: number
  content: string
}

export interface RagQueryResult {
  result_num: number
  results: RagQueryResultItem[]
}

/**
 * 创建知识库
 */
export const createKnowledgeBase = async (data: KnowledgeBaseCreateDTO): Promise<KnowledgeBase> => {
  return http.post('/v1/knowledge-bases', data)
}

/**
 * 获取知识库列表（支持分页、搜索、筛选）
 */
export const getKnowledgeBaseList = async (params?: {
  page?: number
  pageSize?: number
  search?: string
  scopeType?: string
  accessLevel?: string
}): Promise<PageResult<KnowledgeBase>> => {
  return http.get('/v1/knowledge-bases', { params })
}

/**
 * 获取知识库详情
 */
export const getKnowledgeBase = async (uuid: string): Promise<KnowledgeBase> => {
  return http.get(`/v1/knowledge-bases/${uuid}`)
}

/**
 * 更新知识库（部分更新）
 */
export const updateKnowledgeBase = async (
  uuid: string,
  data: KnowledgeBasePatchDTO
): Promise<KnowledgeBase> => {
  return http.patch(`/v1/knowledge-bases/${uuid}`, data)
}

/**
 * 删除知识库
 */
export const deleteKnowledgeBase = async (uuid: string): Promise<void> => {
  return http.delete(`/v1/knowledge-bases/${uuid}`)
}

/**
 * 获取知识库的文档列表
 */
export const getDocumentList = async (
  kbUuid: string,
  params?: {
    page?: number
    pageSize?: number
    status?: string
  }
): Promise<PageResult<Document>> => {
  return http.get(`/v1/knowledge-bases/${kbUuid}/documents`, { params })
}

/**
 * 上传文档到知识库
 */
export const uploadDocument = async (
  kbUuid: string,
  file: File,
  onUploadProgress?: (progressEvent: any) => void
): Promise<Document> => {
  const formData = new FormData()
  formData.append('file', file)

  return http.post(`/v1/knowledge-bases/${kbUuid}/documents`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress
  })
}

/**
 * 获取文档详情
 */
export const getDocument = async (uuid: string): Promise<Document> => {
  return http.get(`/v1/documents/${uuid}`)
}

/**
 * 删除文档
 */
export const deleteDocument = async (uuid: string): Promise<void> => {
  return http.delete(`/v1/documents/${uuid}`)
}

/**
 * 查询知识库
 */
export const queryKnowledgeBase = async (
  kbUuid: string,
  kb_id: string,
  query: string,
  top_k: number = 5,
  similarity_threshold: number = 0.5): Promise<RagQueryResult> => {
  return http.post(`/v1/knowledge-bases/${kbUuid}/query`, {
    knowledge_base_id: kb_id,
    query: query,
    top_k: top_k,
    similarity_threshold: similarity_threshold
  })
}
