/**
 * 实体类型定义
 * 对应后端 Java Entity 类
 */

/**
 * 智能体插件配置
 * 用于配置智能体关联的插件，包含优先级和启用状态
 */
export interface AgentPluginConfig {
  pluginId: string
  priority: number  // 优先级，数值越大优先级越高
  enabled: boolean  // 是否启用
}

/**
 * 智能体实体
 */
export interface Agent {
  id?: string
  name: string
  description?: string
  prompt?: string
  promptTemplate?: string
  modelConfig?: Record<string, any>
  status?: string // draft/published
  userId?: string
  workflowId?: string // 废弃，使用workflows
  workflows?: string[] // 关联的工作流ID列表
  knowledgeBaseId?: string
  kbIds?: string[]
  toolsConfig?: AgentPluginConfig[] // 插件配置列表（含优先级和启用状态）
  createTime?: string
  updateTime?: string
}

/**
 * 插件实体
 * 同时支持 camelCase 和 snake_case 以兼容后端返回格式
 */
export interface Plugin {
  id?: string
  name: string
  identifier?: string
  description?: string
  type?: string
  baseUrl?: string
  method?: string
  endpoint?: string
  openapiSpec?: any
  openapiSchema?: any
  paramsSchema?: Record<string, any>
  responseSchema?: Record<string, any>
  status?: string
  isEnabled?: boolean
  is_enabled?: boolean
  authInfo?: any
  authType?: string
  authConfig?: any
  operations?: any[]
  userId?: string
  createTime?: string | number
  create_time?: string | number
  updateTime?: string | number
  update_time?: string | number
}

/**
 * 对话元数据
 */
export interface ConversationMetadata {
  llmModelId?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  references?: string[]
  remarks?: string
}

/**
 * 插件调用信息
 */
export interface PluginCall {
  pluginId: string
  operationId: string
  params: Record<string, any>
}

/**
 * 智能体对话历史实体
 */
export interface AgentConversation {
  id?: string
  sessionId: string
  agentId: string
  userId?: string
  query: string
  answer?: string
  metadata?: ConversationMetadata
  conversationType?: string // chat/debug
  createTime?: string
  pluginCall?: PluginCall  // 插件调用信息
}

/**
 * LLM模型实体
 */
export interface LlmModel {
  id?: string
  name: string
  displayName?: string
  provider?: string
  modelType?: string
  apiBase?: string
  apiKey?: string
  apiVersion?: string
  maxTokens?: number
  temperature?: number
  topP?: number
  enableDeepThinking?: boolean
  frequencyPenalty?: number
  presencePenalty?: number
  config?: string
  description?: string
  isActive?: boolean
  isDefault?: boolean
  isSystem?: boolean
  sortOrder?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * LLM提供商实体
 */
export interface LlmProvider {
  id?: string
  code: string
  name: string
  title?: string
  description?: string
  applyUrl?: string
  docUrl?: string
  defaultApiBase?: string
  hasFreeQuota?: boolean
  icon?: string
  tagType?: string
  country?: string
  sortOrder?: number
  isActive?: boolean
  createdAt?: string
  updatedAt?: string
}

