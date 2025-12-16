import { http } from '@/utils/http'

// 工作流节点类型
export interface WorkflowNode {
  id: string
  type: string
  label: string
  position: {
    x: number
    y: number
  }
  data?: any
  config?: any
}

// 工作流边类型
export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  condition?: {
    type: string
    field?: string
    value?: string
  }
}

// 工作流类型
export interface Workflow {
  id?: string
  uuid?: string
  agentId?: string
  name: string
  description?: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
  config?: Record<string, any>
  isValid?: boolean
  isActive?: boolean
  isPublic?: boolean
  executionCount?: number
  successCount?: number
  userId?: string
  createTime?: string
  updateTime?: string
}

// 工作流列表响应（与后端API对齐）
export interface WorkflowListResponse {
  total: number
  items: Workflow[]
}

// 工作流执行请求
export interface WorkflowExecutionRequest {
  input?: Record<string, any>
  llm_model_id?: string
}

// 工作流执行结果（与后端API对齐）
export interface WorkflowExecutionResult {
  execution_id: string
  status: string
  message?: string
}

/**
 * 工作流执行记录（与后端实体对齐）
 * 注意：后端返回的是 snake_case 格式，前端同时支持两种格式以保持兼容性
 */
export interface WorkflowExecution {
  id?: number
  // 执行ID（优先使用 camelCase，兼容 snake_case）
  executionId?: string
  execution_id?: string
  // 工作流ID
  workflowId?: string
  workflow_id?: string
  workflowUuid?: string
  // 用户ID
  userId?: string
  user_id?: string
  // 执行状态
  status: string
  // 输入输出
  input?: Record<string, any>
  output?: Record<string, any>
  // 错误信息（优先使用 camelCase，兼容 snake_case）
  errorMessage?: string
  error_message?: string
  // 执行时间（毫秒，优先使用 camelCase，兼容 snake_case）
  executionTime?: number
  execution_time?: number
  // 开始时间（优先使用 camelCase，兼容 snake_case）
  startedAt?: string
  started_at?: string
  // 完成时间（优先使用 camelCase，兼容 snake_case）
  completedAt?: string
  completed_at?: string
  // 节点执行记录（优先使用 camelCase，兼容 snake_case）
  nodeExecutions?: any[]
  node_executions?: any[]
  // 运行类型
  runType?: string
  run_type?: string
  // 创建时间（优先使用 camelCase，兼容 snake_case）
  createTime?: string
  create_time?: string
  // 更新时间（优先使用 camelCase，兼容 snake_case）
  updateTime?: string
  update_time?: string
}

/**
 * 查询工作流列表
 */
export const getWorkflowList = async (params?: {
  page?: number
  pageSize?: number
  search?: string
}): Promise<WorkflowListResponse> => {
  return http.get<WorkflowListResponse>('/v1/workflows', { params })
}

/**
 * 根据UUID查询工作流详情
 */
export const getWorkflowByUuid = async (uuid: string): Promise<Workflow> => {
  return http.get<Workflow>(`/v1/workflows/${uuid}`)
}

/**
 * 创建工作流
 */
export const createWorkflow = async (workflow: Workflow): Promise<Workflow> => {
  return http.post<Workflow>('/v1/workflows', workflow)
}

/**
 * 更新工作流
 */
export const updateWorkflow = async (uuid: string, workflow: Workflow): Promise<Workflow> => {
  return http.put<Workflow>(`/v1/workflows/${uuid}`, workflow)
}

/**
 * 删除工作流
 */
export const deleteWorkflow = async (uuid: string): Promise<void> => {
  return http.delete<void>(`/v1/workflows/${uuid}`)
}

// 工作流验证结果（兼容后端返回的snake_case格式）
export interface WorkflowValidationResult {
  valid: boolean
  errorMessage?: string
  error_message?: string // 后端返回的snake_case格式
  warnings?: string[]
  validationDetails?: {
    hasStartNode?: boolean
    hasEndNode?: boolean
    hasCycle?: boolean
    unreachableNodes?: string[]
  }
  validation_details?: { // 后端返回的snake_case格式
    has_start_node?: boolean
    has_end_node?: boolean
    has_cycle?: boolean
    unreachable_nodes?: string[]
  }
}

/**
 * 验证工作流
 */
export const validateWorkflow = async (uuid: string): Promise<WorkflowValidationResult> => {
  return http.post<WorkflowValidationResult>(`/v1/workflows/${uuid}/validate`)
}

/**
 * 执行工作流
 */
export const executeWorkflow = async (
  uuid: string,
  request: WorkflowExecutionRequest
): Promise<WorkflowExecutionResult> => {
  // 后端期望的格式：{ input: {...}, llm_model_id: "..." }
  const payload = {
    input: request.input || {},
    llm_model_id: request.llm_model_id
  }
  return http.post<WorkflowExecutionResult>(`/v1/workflows/${uuid}/execute`, payload)
}

/**
 * 获取执行记录详情
 */
export const getExecution = async (executionId: string): Promise<WorkflowExecution> => {
  return http.get<WorkflowExecution>(`/v1/workflows/executions/${executionId}`)
}

/**
 * 获取工作流执行历史列表
 */
export const getWorkflowExecutions = async (
  uuid: string,
  params?: {
    page?: number
    pageSize?: number
    status?: string
  }
): Promise<{ total: number; items: WorkflowExecution[]; workflow_uuid?: string; workflow_name?: string }> => {
  return http.get<{ total: number; items: WorkflowExecution[]; workflow_uuid?: string; workflow_name?: string }>(`/v1/workflows/${uuid}/executions`, { params })
}

