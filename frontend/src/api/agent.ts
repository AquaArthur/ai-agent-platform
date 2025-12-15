import { http } from '@/utils/http'
import type { Agent } from '@/types/entity'

/**
 * 查询所有智能体
 */
export const getAgentList = async (): Promise<Agent[]> => {
  return http.get<Agent[]>('/v1/agents')
}

/**
 * 根据ID查询智能体
 */
export const getAgentById = async (id: string): Promise<Agent> => {
  return http.get<Agent>(`/v1/agents/${id}`)
}

/**
 * 创建智能体
 */
export const createAgent = async (agent: Agent): Promise<Agent> => {
  return http.post<Agent>('/v1/agents', agent)
}

/**
 * 更新智能体
 */
export const updateAgent = async (id: string, agent: Agent): Promise<Agent> => {
  return http.put<Agent>(`/v1/agents/${id}`, agent)
}

/**
 * 删除智能体
 */
export const deleteAgent = async (id: string): Promise<void> => {
  return http.delete(`/v1/agents/${id}`)
}

