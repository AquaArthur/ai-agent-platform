import { http } from '@/utils/http'
import type { Plugin } from '@/types/entity'

/**
 * 查询所有插件
 */
export const getPluginList = async (): Promise<Plugin[]> => {
  // 后端返回分页格式 { list: [...], total: n }，这里提取 list
  const response: any = await http.get<{ list: Plugin[] }>('/v1/plugins')
  const list = response?.list || response || []

  // 后端返回 is_enabled (下划线)，前端使用 isEnabled (驼峰)，需要映射
  return list.map((item: any) => ({
    ...item,
    isEnabled: item.is_enabled ?? item.isEnabled ?? false,
    createTime: item.create_time || item.createTime,
    updateTime: item.update_time || item.updateTime
  }))
}

/**
 * 根据ID查询插件
 */
export const getPluginById = async (id: string): Promise<Plugin> => {
  return http.get<Plugin>(`/v1/plugins/${id}`)
}

/**
 * 创建插件
 * 如果 openapiSpec 包含 paths，自动使用 OpenAPI 导入接口
 */
export const createPlugin = async (plugin: Plugin): Promise<Plugin> => {
  const spec = plugin.openapiSpec
  const hasOpenApiPaths =
    spec &&
    typeof spec === 'object' &&
    'paths' in spec &&
    Object.keys((spec as any).paths || {}).length > 0

  if (hasOpenApiPaths) {
    return importFromOpenApi(plugin)
  }

  return http.post<Plugin>('/v1/plugins', plugin)
}

/**
 * 通过 OpenAPI 规范导入插件
 * 会自动解析 paths 创建 operations
 */
export const importFromOpenApi = async (plugin: Plugin): Promise<Plugin> => {
  const spec = plugin.openapiSpec as any
  const baseUrl = spec?.servers?.[0]?.url || ''

  const importRequest = {
    name: plugin.name,
    description: plugin.description || spec?.info?.description || '',
    type: 'openapi',
    baseUrl,
    openapiSpec: spec,
    authType: plugin.authType || 'none',
    authConfig: plugin.authConfig || {}
  }

  return http.post<Plugin>('/v1/plugins/import-openapi', importRequest)
}

/**
 * 更新插件
 */
export const updatePlugin = async (id: string, plugin: Plugin): Promise<Plugin> => {
  return http.put<Plugin>(`/v1/plugins/${id}`, plugin)
}

/**
 * 删除插件
 */
export const deletePlugin = async (id: string): Promise<void> => {
  return http.delete(`/v1/plugins/${id}`)
}

/**
 * 更新插件状态（启用/禁用）
 */
export const updatePluginStatus = async (id: string, isEnabled: boolean): Promise<Plugin> => {
  return http.patch(`/v1/plugins/${id}/status`, { isEnabled })
}

/**
 * 获取插件的操作列表
 */
export const getPluginOperations = async (pluginId: string): Promise<any[]> => {
  const response: any = await http.get(`/v1/plugins/${pluginId}`)
  return response?.operations || []
}

/**
 * 调用插件操作
 */
export const invokePluginOperation = async (
  pluginId: string,
  operationId: string,
  params: Record<string, any>,
  timeout?: number
): Promise<any> => {
  return http.post(`/v1/plugins/${pluginId}/operations/${operationId}/invoke`, {
    params,
    timeout: timeout ?? 30000
  })
}

