import { http } from '@/utils/http'
import type { AgentConversation } from '@/types/entity'

export interface CreateSessionResponse {
  session_id: string
}

/**
 * 创建会话
 */
export const createSession = async (): Promise<CreateSessionResponse> => {
  return http.post<CreateSessionResponse>('/v1/chat/session')
}

/**
 * 获取消息历史
 */
export const getMessageHistory = async (sessionId: string): Promise<AgentConversation[]> => {
  return http.get<AgentConversation[]>(`/v1/chat/history/${sessionId}`)
}

/**
 * 发送消息
 * AI 对话可能涉及多轮 LLM 调用和插件调用，需要更长的超时时间
 */
export const sendMessage = async (conversation: AgentConversation): Promise<AgentConversation> => {
  return http.post<AgentConversation>('/v1/chat/message', conversation, {
    timeout: 1200000 // 20 分钟超时
  })
}
