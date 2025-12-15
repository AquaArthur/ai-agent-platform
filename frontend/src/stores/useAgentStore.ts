import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Agent } from '@/types/entity'
import {
  getAgentList,
  getAgentById,
  createAgent,
  updateAgent,
  deleteAgent
} from '@/api'
import { withLoading as withLoadingUtil } from '@/utils/store'

/**
 * 智能体 Store
 * 管理智能体的状态和 CRUD 操作
 */
export const useAgentStore = defineStore('agent', () => {
  // 状态
  const agentList = ref<Agent[]>([])
  const loading = ref(false)
  const currentAgent = ref<Agent | null>(null)

  // 异步操作包装器
  const withLoading = <T>(operation: () => Promise<T>, errorMessage: string) =>
    withLoadingUtil(loading, operation, errorMessage)

  /**
   * 获取智能体列表
   */
  const fetchAgentList = async (): Promise<Agent[]> => {
    return withLoading(async () => {
      agentList.value = await getAgentList()
      return agentList.value
    }, '获取智能体列表失败:')
  }

  /**
   * 根据ID获取智能体详情
   */
  const fetchAgentById = async (id: string): Promise<Agent> => {
    return withLoading(async () => {
      currentAgent.value = await getAgentById(id)
      return currentAgent.value
    }, '获取智能体详情失败:')
  }

  /**
   * 创建智能体
   */
  const addAgent = async (agent: Agent): Promise<Agent> => {
    return withLoading(async () => {
      const created = await createAgent(agent)
      await fetchAgentList()
      return created
    }, '创建智能体失败:')
  }

  /**
   * 更新智能体
   */
  const editAgent = async (id: string, agent: Agent): Promise<Agent> => {
    return withLoading(async () => {
      const updated = await updateAgent(id, agent)
      await fetchAgentList()
      return updated
    }, '更新智能体失败:')
  }

  /**
   * 删除智能体
   */
  const removeAgent = async (id: string): Promise<void> => {
    return withLoading(async () => {
      await deleteAgent(id)
      await fetchAgentList()
    }, '删除智能体失败:')
  }

  /**
   * 重置当前智能体
   */
  const resetCurrentAgent = (): void => {
    currentAgent.value = null
  }

  return {
    // 状态
    agentList,
    loading,
    currentAgent,
    // 方法
    fetchAgentList,
    fetchAgentById,
    addAgent,
    editAgent,
    removeAgent,
    resetCurrentAgent
  }
})

