import { http, USE_MOCK } from '@/utils/http'

/**
 * 后端统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * Hello API - 前后端连通性测试
 * 当 USE_MOCK 为 true 时，返回模拟响应，用于测试前端功能
 */
export const getHello = async (): Promise<string> => {
  if (USE_MOCK) {
    return Promise.resolve('Hello from Mock Server!')
  }
  return http.get<string>('/v1/hello')
}

// 导出所有 API
export * from './agent'
export * from './plugin'
export * from './chat'
export * from './llm'
export * from './knowledgeBase'
export * from './workflow'

// 导出类型定义
export * from '@/types/entity'
