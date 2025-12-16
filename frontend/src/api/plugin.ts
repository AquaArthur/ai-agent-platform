import { http } from '@/utils/http'
import type { Plugin } from '@/types/entity'

/**
 * 查询所有插件（返回分页结果）
 */
export const getPluginList = async (params?: {
  page?: number
  pageSize?: number
}): Promise<{ list: Plugin[]; total: number }> => {
  return http.get<{ list: Plugin[]; total: number }>('/v1/plugins', { params })
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
  return http.patch<Plugin>(`/v1/plugins/${id}/status`, { isEnabled })
}

/**
 * 获取插件的操作列表
 */
export const getPluginOperations = async (pluginId: string): Promise<any[]> => {
  return http.get<any[]>(`/v1/plugins/${pluginId}/operations`)
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

