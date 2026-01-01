import { http } from '@/utils/http'
import type { Agent } from '@/types/entity'
import type { KnowledgeBase } from './knowledgeBase'

// ==================== 智能体基础 CRUD ====================

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

// ==================== 智能体-知识库关联管理 ====================

/**
 * 智能体-知识库关联实体
 */
export interface AgentKnowledgeBase {
  id?: string
  agentId: string
  knowledgeBaseId: string
  priority?: number
  enabled?: boolean
  createTime?: string
  updateTime?: string
}

/**
 * 为智能体添加知识库关联
 * @param agentId 智能体ID
 * @param knowledgeBaseId 知识库ID
 * @param priority 优先级（可选）
 */
export const addKnowledgeBaseToAgent = async (
  agentId: string,
  knowledgeBaseId: string,
  priority?: number
): Promise<AgentKnowledgeBase> => {
  return http.post<AgentKnowledgeBase>(`/v1/agents/${agentId}/knowledge-bases`, {
    knowledgeBaseId,
    priority
  })
}

/**
 * 批量为智能体添加知识库关联
 * @param agentId 智能体ID
 * @param knowledgeBaseIds 知识库ID列表
 */
export const batchAddKnowledgeBasesToAgent = async (
  agentId: string,
  knowledgeBaseIds: string[]
): Promise<AgentKnowledgeBase[]> => {
  return http.post<AgentKnowledgeBase[]>(`/v1/agents/${agentId}/knowledge-bases/batch`, {
    knowledgeBaseIds
  })
}

/**
 * 获取智能体关联的所有知识库
 * @param agentId 智能体ID
 * @param enabledOnly 是否只获取启用的知识库
 */
export const getAgentKnowledgeBases = async (
  agentId: string,
  enabledOnly: boolean = false
): Promise<KnowledgeBase[]> => {
  return http.get<KnowledgeBase[]>(`/v1/agents/${agentId}/knowledge-bases`, {
    params: { enabledOnly }
  })
}

/**
 * 移除智能体的知识库关联
 * @param agentId 智能体ID
 * @param knowledgeBaseId 知识库ID
 */
export const removeKnowledgeBaseFromAgent = async (
  agentId: string,
  knowledgeBaseId: string
): Promise<void> => {
  return http.delete<void>(`/v1/agents/${agentId}/knowledge-bases/${knowledgeBaseId}`)
}

/**
 * 批量移除智能体的知识库关联
 * @param agentId 智能体ID
 * @param knowledgeBaseIds 知识库ID列表
 */
export const batchRemoveKnowledgeBasesFromAgent = async (
  agentId: string,
  knowledgeBaseIds: string[]
): Promise<void> => {
  return http.delete<void>(`/v1/agents/${agentId}/knowledge-bases/batch`, {
    data: { knowledgeBaseIds }
  })
}

/**
 * 更新知识库关联的优先级
 * @param agentId 智能体ID
 * @param knowledgeBaseId 知识库ID
 * @param priority 新的优先级
 */
export const updateKnowledgeBasePriority = async (
  agentId: string,
  knowledgeBaseId: string,
  priority: number
): Promise<AgentKnowledgeBase> => {
  return http.patch<AgentKnowledgeBase>(
    `/v1/agents/${agentId}/knowledge-bases/${knowledgeBaseId}/priority`,
    { priority }
  )
}

/**
 * 切换知识库关联的启用状态
 * @param agentId 智能体ID
 * @param knowledgeBaseId 知识库ID
 * @param enabled 是否启用
 */
export const toggleKnowledgeBaseEnabled = async (
  agentId: string,
  knowledgeBaseId: string,
  enabled: boolean
): Promise<AgentKnowledgeBase> => {
  return http.patch<AgentKnowledgeBase>(
    `/v1/agents/${agentId}/knowledge-bases/${knowledgeBaseId}/toggle`,
    { enabled }
  )
}

/**
 * 检查智能体是否关联了指定知识库
 * @param agentId 智能体ID
 * @param knowledgeBaseId 知识库ID
 */
export const checkKnowledgeBaseAssociation = async (
  agentId: string,
  knowledgeBaseId: string
): Promise<boolean> => {
  return http.get<boolean>(`/v1/agents/${agentId}/knowledge-bases/${knowledgeBaseId}/exists`)
}

/**
 * 同步智能体的知识库关联（智能对比并更新）
 * 该方法会对比当前关联和目标关联，自动添加/移除差异项
 * @param agentId 智能体ID
 * @param targetKbIds 目标知识库ID列表
 */
export const syncAgentKnowledgeBases = async (
  agentId: string,
  targetKbIds: string[]
): Promise<void> => {
  // 1. 获取当前关联的知识库
  const currentKbs = await getAgentKnowledgeBases(agentId)
  const currentKbIds = currentKbs.map(kb => kb.id || kb.uuid)

  // 2. 计算需要添加和移除的知识库
  const toAdd = targetKbIds.filter(id => !currentKbIds.includes(id))
  const toRemove = currentKbIds.filter(id => !targetKbIds.includes(id))

  // 3. 批量添加新关联
  if (toAdd.length > 0) {
    await batchAddKnowledgeBasesToAgent(agentId, toAdd)
  }

  // 4. 批量移除旧关联
  if (toRemove.length > 0) {
    await batchRemoveKnowledgeBasesFromAgent(agentId, toRemove)
  }
}

