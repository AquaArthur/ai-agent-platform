import { http } from '@/utils/http'
import type { LlmModel, LlmProvider } from '@/types/entity'

/**
 * 查询所有 LLM 模型
 */
export const getLlmModelList = async (): Promise<LlmModel[]> => {
  return http.get<LlmModel[]>('/v1/llm/models')
}

/**
 * 根据ID查询 LLM 模型
 */
export const getLlmModelById = async (id: string): Promise<LlmModel> => {
  return http.get<LlmModel>(`/v1/llm/models/${id}`)
}

/**
 * 创建 LLM 模型
 */
export const createLlmModel = async (model: LlmModel): Promise<LlmModel> => {
  return http.post<LlmModel>('/v1/llm/models', model)
}

/**
 * 更新 LLM 模型
 */
export const updateLlmModel = async (id: string, model: LlmModel): Promise<LlmModel> => {
  return http.put<LlmModel>(`/v1/llm/models/${id}`, model)
}

/**
 * 删除 LLM 模型
 */
export const deleteLlmModel = async (id: string): Promise<void> => {
  return http.delete<void>(`/v1/llm/models/${id}`)
}

/**
 * 查询所有 LLM 提供商
 */
export const getLlmProviderList = async (): Promise<LlmProvider[]> => {
  return http.get<LlmProvider[]>('/v1/llm/providers')
}
