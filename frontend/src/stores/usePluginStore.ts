import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Plugin } from '@/types/entity'
import {
  getPluginList,
  getPluginById,
  createPlugin,
  updatePlugin,
  deletePlugin,
  updatePluginStatus,
  getPluginOperations,
  invokePluginOperation
} from '@/api'
import { withLoading as withLoadingUtil } from '@/utils/store'

/**
 * 插件 Store
 * 管理插件的状态和 CRUD 操作
 */
export const usePluginStore = defineStore('plugin', () => {
  // 状态
  const pluginList = ref<Plugin[]>([])
  const loading = ref(false)
  const currentPlugin = ref<Plugin | null>(null)

  // 异步操作包装器
  const withLoading = <T>(operation: () => Promise<T>, errorMessage: string) =>
    withLoadingUtil(loading, operation, errorMessage)

  /**
   * 获取插件列表
   */
  const fetchPluginList = async (): Promise<Plugin[]> => {
    return withLoading(async () => {
      pluginList.value = await getPluginList()
      return pluginList.value
    }, '获取插件列表失败:')
  }

  /**
   * 根据ID获取插件详情
   */
  const fetchPluginById = async (id: string): Promise<Plugin> => {
    return withLoading(async () => {
      currentPlugin.value = await getPluginById(id)
      return currentPlugin.value
    }, '获取插件详情失败:')
  }

  /**
   * 创建插件
   */
  const addPlugin = async (plugin: Plugin): Promise<Plugin> => {
    return withLoading(async () => {
      const created = await createPlugin(plugin)
      await fetchPluginList()
      return created
    }, '创建插件失败:')
  }

  /**
   * 更新插件
   */
  const editPlugin = async (id: string, plugin: Plugin): Promise<Plugin> => {
    return withLoading(async () => {
      const updated = await updatePlugin(id, plugin)
      await fetchPluginList()
      return updated
    }, '更新插件失败:')
  }

  /**
   * 删除插件
   */
  const removePlugin = async (id: string): Promise<void> => {
    return withLoading(async () => {
      await deletePlugin(id)
      await fetchPluginList()
    }, '删除插件失败:')
  }

  /**
   * 重置当前插件
   */
  const resetCurrentPlugin = (): void => {
    currentPlugin.value = null
  }

  /**
   * 更新插件状态（启用/禁用）
   */
  const togglePluginStatus = async (id: string, isEnabled: boolean): Promise<void> => {
    return withLoading(async () => {
      await updatePluginStatus(id, isEnabled)
      await fetchPluginList()
    }, '更新插件状态失败:')
  }

  /**
   * 获取插件操作列表
   */
  const fetchPluginOperations = async (pluginId: string) => {
    try {
      return await getPluginOperations(pluginId)
    } catch (error) {
      console.error('获取插件操作失败:', error)
      throw error
    }
  }

  /**
   * 调用插件操作
   */
  const invokeOperation = async (
    pluginId: string,
    operationId: string,
    params: Record<string, any>,
    timeout?: number
  ) => {
    try {
      return await invokePluginOperation(pluginId, operationId, params, timeout)
    } catch (error) {
      console.error('调用插件操作失败:', error)
      throw error
    }
  }

  return {
    // 状态
    pluginList,
    loading,
    currentPlugin,
    // 方法
    fetchPluginList,
    fetchPluginById,
    addPlugin,
    editPlugin,
    removePlugin,
    resetCurrentPlugin,
    togglePluginStatus,
    fetchPluginOperations,
    invokeOperation
  }
})

